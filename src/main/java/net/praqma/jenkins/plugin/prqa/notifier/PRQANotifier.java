package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.tuple.Pair;
import jenkins.model.ArtifactManager;
import jenkins.model.Jenkins;
import jenkins.util.BuildListenerAdapter;
import net.praqma.jenkins.plugin.prqa.PRQARemoteToolCheck;
import net.praqma.jenkins.plugin.prqa.QAFrameworkRemoteReport;
import net.praqma.jenkins.plugin.prqa.QAFrameworkRemoteReportUpload;
import net.praqma.jenkins.plugin.prqa.VersionInfo;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.graphs.ComplianceIndexGraphs;
import net.praqma.jenkins.plugin.prqa.graphs.MessagesGraph;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.jenkins.plugin.prqa.notifier.slave.filesystem.CopyReportsToWorkspace;
import net.praqma.jenkins.plugin.prqa.notifier.slave.filesystem.DeleteReportsFromWorkspace;
import net.praqma.jenkins.plugin.prqa.setup.PRQAToolSuite;
import net.praqma.jenkins.plugin.prqa.setup.QAFrameworkInstallationConfiguration;
import net.praqma.jenkins.plugin.prqa.threshold.AbstractThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.FileComplianceThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.MessageComplianceThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.ProjectComplianceThreshold;
import net.praqma.jenkins.plugin.prqa.utils.PRQABuildUtils;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.QAVerifyServerSettings;
import net.praqma.prqa.QaFrameworkVersion;
import net.praqma.prqa.ReportSettings;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.products.QACli;
import net.praqma.prqa.reports.QAFrameworkReport;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.StatusCategory;
import net.praqma.prqa.qaframework.QaFrameworkReportSettings;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.praqma.prqa.QaFrameworkVersion.MINIMUM_SUPPORTED_VERSION;
import static net.praqma.prqa.reports.ReportType.CRR;
import static net.praqma.prqa.reports.ReportType.MDR;
import static net.praqma.prqa.reports.ReportType.RCR;
import static net.praqma.prqa.reports.ReportType.SUR;

public class PRQANotifier
        extends Recorder
        implements Serializable, SimpleBuildStep {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(PRQANotifier.class.getName());

    protected transient PrintStream outStream;
    protected List<PRQAGraph> graphTypes;

    public QAFrameworkPostBuildActionSetup sourceQAFramework;

    public final List<AbstractThreshold> thresholdsDesc;

    public final boolean runWhenSuccess;

    public EnumSet<QARReportType> chosenReportTypes;

    @DataBoundConstructor
    public PRQANotifier(final boolean runWhenSuccess,
                        final QAFrameworkPostBuildActionSetup sourceQAFramework,
                        final List<AbstractThreshold> thresholdsDesc) {

        this.runWhenSuccess = runWhenSuccess;
        this.sourceQAFramework = sourceQAFramework;
        this.thresholdsDesc = thresholdsDesc;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new PRQAProjectAction(project);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    protected void copyResourcesToArtifactsDir(String pattern,
                                               Run<?, ?> build,
                                               FilePath workspace,
                                               TaskListener listener)
            throws IOException, InterruptedException {

        if (workspace == null) {
            return;
        }

        FilePath[] files = workspace.list("**/" + pattern);

        Map<String, String> artifacts = new HashMap<>();

        for (FilePath file : files) {
            artifacts.put(file.getName(), StringUtils.replace(file.getRemote(), workspace.getRemote(), ""));
        }

        if (artifacts.isEmpty()) {
            return;
        }

        BuildListenerAdapter adapter = new BuildListenerAdapter(listener);
        ArtifactManager artifactManager = build.pickArtifactManager();
        Launcher launcher = workspace.createLauncher(adapter);

        artifactManager.archive(workspace, launcher, adapter, artifacts);
    }

    /**
     * Process the results
     */
    protected boolean evaluate(PRQAReading previousStableComplianceStatus,
                               List<? extends AbstractThreshold> thresholds,
                               PRQAComplianceStatus currentComplianceStatus) {
        PRQAComplianceStatus previousComplianceStatus = (PRQAComplianceStatus) previousStableComplianceStatus;
        HashMap<StatusCategory, Number> tholds = new HashMap<>();
        if (thresholds == null) {
            return true;
        }
        for (AbstractThreshold threshold : thresholds) {
            if (threshold.improvement) {
                if (!isBuildStableForContinuousImprovement(threshold, currentComplianceStatus,
                        previousComplianceStatus)) {
                    return false;
                }
            } else {
                addThreshold(threshold, tholds);
                currentComplianceStatus.setThresholds(tholds);
                if (!threshold.validate(previousComplianceStatus, currentComplianceStatus)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isBuildStableForContinuousImprovement(AbstractThreshold threshold,
                                                            PRQAComplianceStatus currentComplianceStatus,
                                                            PRQAComplianceStatus previousComplianceStatus) {
        if (threshold instanceof MessageComplianceThreshold) {
            if (currentComplianceStatus.getMessages() > previousComplianceStatus.getMessages()) {
                currentComplianceStatus.addNotification(
                        Messages.PRQANotifier_MaxMessagesContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getMessages(), currentComplianceStatus.getMessages()));
                return false;
            }
        } else if (threshold instanceof FileComplianceThreshold) {
            if (currentComplianceStatus.getFileCompliance() < previousComplianceStatus.getFileCompliance()) {
                currentComplianceStatus.addNotification(
                        Messages.PRQANotifier_FileComplianceContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getFileCompliance() + "%",
                                currentComplianceStatus.getFileCompliance()) + "%");
                return false;
            }
        } else if (threshold instanceof ProjectComplianceThreshold) {
            if (currentComplianceStatus.getProjectCompliance() < previousComplianceStatus.getProjectCompliance()) {
                currentComplianceStatus.addNotification(
                        Messages.PRQANotifier_ProjectComplianceContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getProjectCompliance() + "%",
                                currentComplianceStatus.getProjectCompliance()) + "%");
                return false;
            }
        }
        return true;
    }

    /**
     * This method is needed to add the necessary values when drawing the
     * threshold graphs
     */
    // TODO: Refactor this away as soon as possible.
    private void addThreshold(AbstractThreshold threshold,
                              HashMap<StatusCategory, Number> tholds) {
        if (threshold instanceof ProjectComplianceThreshold) {
            tholds.put(StatusCategory.ProjectCompliance, ((ProjectComplianceThreshold) threshold).value);
        } else if (threshold instanceof FileComplianceThreshold) {
            tholds.put(StatusCategory.FileCompliance, ((FileComplianceThreshold) threshold).value);
        } else {
            tholds.put(StatusCategory.Messages, ((MessageComplianceThreshold) threshold).value);
        }
    }

    private void copyReportsToArtifactsDir(ReportSettings settings,
                                           Run<?, ?> run,
                                           FilePath workspace,
                                           TaskListener listener)
            throws IOException, InterruptedException {

        if (workspace == null) {
            throw new IOException("Invalid workspace");
        }

        if (settings instanceof QaFrameworkReportSettings) {

            QaFrameworkReportSettings qaFrameworkSettings = (QaFrameworkReportSettings) settings;

            copyGeneratedReportsToJobWorkspace(run, workspace, qaFrameworkSettings.getQaProject(),
                    qaFrameworkSettings.getProjectConfiguration());
            copyReportsFromWorkspaceToArtifactsDir(run, workspace, listener);
        }
    }

    private void copyGeneratedReportsToJobWorkspace(Run<?, ?> run,
                                                    FilePath workspace,
                                                    final String qaProject,
                                                    String projectConfiguration)
            throws IOException, InterruptedException {

        if (workspace == null) {
            throw new IOException("Invalid workspace");
        }

        workspace.act(new CopyReportsToWorkspace(qaProject, projectConfiguration));
    }

    private boolean containsReportName(String fileName) {
        return fileName.contains(CRR.name()) || fileName.contains(SUR.name()) || fileName.contains(
                RCR.name()) || fileName.contains(MDR.name());
    }

    private void copyReportsFromWorkspaceToArtifactsDir(Run<?, ?> run,
                                                        FilePath workspace,
                                                        TaskListener listener)
            throws IOException, InterruptedException {

        // COPY only last generated reports
        if (workspace == null) {
            throw new IOException("Invalid workspace");
        }

        List<FilePath> workspaceFiles = workspace.list();
        if (workspaceFiles.isEmpty()) {
            return;
        }

        Collections.sort(workspaceFiles, new Comparator<FilePath>() {
            @Override
            public int compare(FilePath o1,
                               FilePath o2) {
                try {
                    return Long.compare(o2.lastModified(), o1.lastModified());
                } catch (IOException | InterruptedException e) {
                    return 0;
                }
            }
        });

        Map<String, String> artifacts = new HashMap<>();

        for (FilePath file : workspaceFiles) {
            if (file.lastModified() < run.getTimeInMillis()) {
                break;
            }

            if (containsReportName(file.getName())) {
                artifacts.put(file.getName(), file.getName());
            }
        }

        if (artifacts.isEmpty()) {
            return;
        }

        BuildListenerAdapter adapter = new BuildListenerAdapter(listener);
        Launcher launcher = workspace.createLauncher(adapter);
        ArtifactManager artifactManager = run.pickArtifactManager();

        artifactManager.archive(workspace, launcher, adapter, artifacts);

    }

    public List<PRQAGraph> getSupportedGraphs() {
        if (graphTypes == null || graphTypes.size() == 0) {
            if (CollectionUtils.isEmpty(this.getGraphTypes())) {
                ArrayList<PRQAGraph> list = new ArrayList<>();
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
                this.setGraphTypes(list);
            }
        }
        ArrayList<PRQAGraph> graphs = new ArrayList<>();
        for (PRQAGraph g : graphTypes) {
            if (g.getType()
                    .equals(QARReportType.Compliance)) {
                graphs.add(g);
            }
        }
        return graphs;
    }

    public PRQAGraph getGraph(String simpleClassName) {
        for (PRQAGraph p : getSupportedGraphs()) {
            if (p.getClass()
                    .getSimpleName()
                    .equals(simpleClassName)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {

        outStream = taskListener.getLogger();

        cleanWorkspace(run, workspace, taskListener);

        if (run instanceof FreeStyleBuild) {

            Result buildResult = run.getResult();

            if (buildResult == null) {
                log.log(WARNING, "Build result is unavailable at this point.");
                outStream.println("Build result is unavailable at this point. Will not proceed.");
                return;
            } else if (buildResult.isWorseOrEqualTo(FAILURE)) {
                log.log(WARNING, "Build is marked as failure.");
                if (runWhenSuccess) {
                    outStream.println("Build is marked as failure and Helix QAC Analysis will not proceed.");
                    return;
                }
                outStream.println("Build is marked as failure but Helix QAC Analysis will proceed.");
            }
        }

        performQaFrameworkBuild(run, workspace, launcher, taskListener);
    }

    private void cleanWorkspace(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener taskListener) throws AbortException {
        boolean success = false;

        if (workspace == null) {
            taskListener.getLogger()
                    .println("Invalid workspace. Cannot continue.");
            throw new AbortException("Invalid workspace. Cannot continue.");
        }

        DeleteReportsFromWorkspace deleter = new DeleteReportsFromWorkspace();

        try {
            success = workspace.act(deleter);
        } catch (IOException | InterruptedException ex) {
            log.log(SEVERE, "Cleanup crew missing!", ex);
            taskListener.getLogger()
                    .println(ex.getMessage());
        }

        if (!success) {
            taskListener.getLogger()
                    .println("Failed to cleanup workspace reports.");
            run.setResult(FAILURE);
            throw new AbortException("Failed to cleanup workspace reports.");
        }
    }

    public EnumSet<QARReportType> getChosenReportTypes() {
        return chosenReportTypes;
    }

    public void setChosenReportTypes(EnumSet<QARReportType> chosenReport) {
        this.chosenReportTypes = chosenReport;
    }

    public boolean enter() {
        return true;
    }

    /**
     * Fetches the most 'previous' result. The current build is baseline. So any
     * prior build to the passed current build is considered.
     */
    private Pair<PRQAReading, Run<?, ?>> getPreviousReading(Run<?, ?> currentBuild,
                                                            Result expectedResult) {
        Run<?, ?> iterate = currentBuild;
        do {
            iterate = iterate.getPreviousNotFailedBuild();
            if (iterate != null && iterate.getAction(PRQABuildAction.class) != null && Objects.equals(
                    iterate.getResult(), expectedResult)) {
                Run<?, ?> second = iterate;
                Pair<PRQAReading, Run<?, ?>> result =
                        Pair.of(iterate.getAction(PRQABuildAction.class).getResult(), second);
                return result;
            }
        } while (iterate != null);
        return null;
    }

    @Exported
    public void setGraphTypes(List<PRQAGraph> graphTypes) {
        this.graphTypes = graphTypes;
    }

    @Exported
    public List<PRQAGraph> getGraphTypes() {
        return graphTypes;
    }


    /**
     * This class is used by Jenkins to define the plugin.
     *
     * @author jes *
     */
    @Extension
    @Symbol("qacReport")
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {

        public List<ThresholdSelectionDescriptor<?>> getThresholdSelections() {
            return AbstractThreshold.getDescriptors();
        }

        @Override
        public String getDisplayName() {
            return "Helix QAC Report";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public Publisher newInstance(StaplerRequest req,
                                     @Nonnull
                                             JSONObject formData)
                throws Descriptor.FormException {

            if (req == null) {
                throw new FormException(new Exception("Bad request"), "Bad request");
            }

            final String SOURCE_QA_FRAMEWORK = "sourceQAFramework";
            final String CODE_REVIEW_REPORT = "generateCrr";
            final String SUPPRESSION_REPORT = "generateSup";
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);
            instance.setChosenReportTypes(QARReportType.REQUIRED_TYPES.clone());

            if (formData.containsKey(SOURCE_QA_FRAMEWORK)) {
                JSONObject sourceObject = formData.getJSONObject(SOURCE_QA_FRAMEWORK);
                if (sourceObject.getBoolean(CODE_REVIEW_REPORT)) {
                    instance.chosenReportTypes.add(QARReportType.CodeReview);

                }
                if (sourceObject.getBoolean(SUPPRESSION_REPORT)) {
                    instance.chosenReportTypes.add(QARReportType.Suppression);
                }
            }
            defineGraphs(instance);

            save();
            return instance;
        }

        private void defineGraphs(PRQANotifier instance) {
            if (CollectionUtils.isEmpty(instance.getGraphTypes())) {
                ArrayList<PRQAGraph> list = new ArrayList<>();
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
                instance.setGraphTypes(list);
            }
        }

        @Override
        public boolean configure(StaplerRequest req,
                                 JSONObject json)
                throws Descriptor.FormException {
            save();
            return super.configure(req, json);
        }

        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }

        public List<QAFrameworkInstallationConfiguration> getQaFrameworkTools() {

            Jenkins jenkins = Jenkins.get();

            QAFrameworkInstallationConfiguration[] prqaInstallations = jenkins.getDescriptorByType(
                    QAFrameworkInstallationConfiguration.DescriptorImpl.class)
                    .getInstallations();
            return Arrays.asList(prqaInstallations);
        }

        public List<PRQAReportSourceDescriptor<?>> getReportSources() {
            return PostBuildActionSetup.getDescriptors();
        }

        public PRQAReportSourceDescriptor<?> getReportSource() {
            return PostBuildActionSetup.getDescriptors()
                    .get(0);
        }
    }


    private void performQaFrameworkBuild(Run<?, ?> run,
                                         FilePath workspace,
                                         Launcher launcher,
                                         TaskListener listener)
            throws IOException, InterruptedException {

        Node node = run.getExecutor().getOwner().getNode();

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup = sourceQAFramework;
        QAFrameworkInstallationConfiguration qaFrameworkInstallationConfiguration = QAFrameworkInstallationConfiguration.getInstallationByName(
                qaFrameworkPostBuildActionSetup.qaInstallation);

        if (qaFrameworkInstallationConfiguration == null) {
            String msg = String.format(
                    "The job uses a Helix QAC Framework installation (%s) that is mis-configured or no longer exists, please reconfigure.",
                    qaFrameworkPostBuildActionSetup.qaInstallation);
            log.log(SEVERE, msg);
            run.setResult(FAILURE);
            throw new AbortException(msg);
        }

        qaFrameworkInstallationConfiguration = qaFrameworkInstallationConfiguration.forNode(node, listener);

        outStream.println(String.format("Perforce Helix QAC Plugin version %s",
                VersionInfo.getPluginVersion()));

        PRQAToolSuite suite = qaFrameworkInstallationConfiguration;

        outStream.println(Messages.PRQANotifier_ReportGenerateText());
        outStream.println("Workspace : " + workspace.getRemote());

        HashMap<String, String> environmentVariables;

        environmentVariables = suite.createEnvironmentVariables(workspace.getRemote());

        PRQAApplicationSettings appSettings = new PRQAApplicationSettings(
                qaFrameworkInstallationConfiguration.getHome());
        QaFrameworkReportSettings qaReportSettings;
        try {
            qaReportSettings = setQaFrameworkReportSettings(qaFrameworkPostBuildActionSetup, run, workspace, listener);
        } catch (PrqaSetupException ex) {
            log.log(SEVERE, ex.getMessage(), ex);
            run.setResult(FAILURE);
            throw new AbortException(ex.getMessage());
        }

        Collection<QAFrameworkRemoteReport> remoteReports = new ArrayList<>();

        Collection<QAFrameworkRemoteReportUpload> remoteReportUploads = new ArrayList<>();

        if (qaFrameworkPostBuildActionSetup.chosenServers != null && !qaFrameworkPostBuildActionSetup.chosenServers.isEmpty()) {
            for (String chosenServer : qaFrameworkPostBuildActionSetup.chosenServers) {
                QAVerifyServerSettings qaVerifySettings = setQaVerifyServerSettings(chosenServer);
                QAFrameworkReport report = new QAFrameworkReport(qaReportSettings, qaVerifySettings,
                        environmentVariables);

                remoteReports.add(new QAFrameworkRemoteReport(report, listener));
                remoteReportUploads.add(new QAFrameworkRemoteReportUpload(report, listener));
            }
        } else {
            QAFrameworkReport report = new QAFrameworkReport(qaReportSettings, setQaVerifyServerSettings(null),
                    environmentVariables);
            remoteReports.add(new QAFrameworkRemoteReport(report, listener));
        }

        PRQARemoteToolCheck remoteToolCheck = new PRQARemoteToolCheck(new QACli(), environmentVariables, appSettings,
                qaReportSettings, listener, launcher.isUnix());
        PRQAComplianceStatus currentBuild;
        try {
            QAFrameworkRemoteReport remoteReport = remoteReports.iterator()
                    .next();

            currentBuild = performBuild(run, remoteToolCheck, remoteReport, qaReportSettings, workspace, listener);
        } catch (PrqaException ex) {
            log.log(SEVERE, ex.getMessage(), ex);
            run.setResult(FAILURE);
            throw new AbortException(ex.getMessage());
        }

        // if Pipeline and above exception not thrown, Helix QAC Analysis has succeeded, so set result for this run to success
        if(!(run instanceof FreeStyleBuild)) run.setResult(SUCCESS);

        Pair<PRQAReading, Run<?, ?>> previousBuildResultTuple = getPreviousReading(run, SUCCESS);

        PRQAReading previousStableBuildResult =
                previousBuildResultTuple != null ? previousBuildResultTuple.getKey() : null;

        log.fine("thresholdsDesc is null: " + (thresholdsDesc == null));
        if (thresholdsDesc != null) {
            log.fine("thresholdsDescSize: " + thresholdsDesc.size());
        }

        if(previousStableBuildResult == null && currentBuild != null) {
            previousStableBuildResult = currentBuild;
        }

        boolean thresholdEvalResult = evaluate(previousStableBuildResult, thresholdsDesc, currentBuild);
        log.fine("Evaluated to: " + thresholdEvalResult);

        Result buildResult = run.getResult();

        if (buildResult == null) {
            log.log(WARNING, "Build result is unavailable at this point.");
            throw new AbortException("Build result is unavailable at this point. Will not proceed.");
        }
        if (!thresholdEvalResult && !buildResult.isWorseOrEqualTo(FAILURE)) {
            run.setResult(UNSTABLE);
        }

        if (qaReportSettings.isLoginToQAV() && qaReportSettings.isPublishToQAV()) {
            if (qaReportSettings.isQaUploadWhenStable() && buildResult.isWorseOrEqualTo(UNSTABLE)) {
                log.log(WARNING, "UPLOAD WARNING - Dashboard Upload cant be performed because the build is Unstable");
                throw new AbortException("Upload warning: Dashboard Upload cant be performed because the build is Unstable");
            } else {
                if (buildResult.isWorseOrEqualTo(UNSTABLE)) {
                    outStream.println("Upload warning: Build is Unstable but upload will continue...");
                }
                outStream.println("Upload info: Dashboard Upload...");
                for (QAFrameworkRemoteReportUpload remoteReportUpload : remoteReportUploads) {
                    try {
                        performUpload(run, workspace, remoteToolCheck, remoteReportUpload);
                    } catch (PrqaException ex) {
                        log.log(SEVERE, ex.getMessage(), ex);
                        run.setResult(FAILURE);
                        throw new AbortException(ex.getMessage());
                    }
                }
            }
        }

        outStream.println("\n----------------------BUILD Results-----------------------\n");
        if (previousBuildResultTuple != null) {
            outStream.println(
                    Messages.PRQANotifier_PreviousResultBuildNumber(previousBuildResultTuple.getValue().number));
            outStream.println(previousBuildResultTuple.getKey());

        } else {
            outStream.println(Messages.PRQANotifier_NoPreviousResults());
        }
        outStream.println(Messages.PRQANotifier_ScannedValues());
        outStream.println(currentBuild);

        PRQABuildAction action = new PRQABuildAction(run);
        action.setResult(currentBuild);
        action.setPublisher(this);
        run.addAction(action);
    }

    private QaFrameworkReportSettings setQaFrameworkReportSettings(QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup,
                                                                   Run<?, ?> run,
                                                                   FilePath workspace,
                                                                   TaskListener listener)
            throws PrqaSetupException {

        if (qaFrameworkPostBuildActionSetup.qaProject == null) {
            throw new PrqaSetupException(
                    "Project configuration is missing. Please set a project in Qa Framework configuration section!");
        }

        return new QaFrameworkReportSettings(qaFrameworkPostBuildActionSetup.qaInstallation,
                qaFrameworkPostBuildActionSetup.useCustomLicenseServer,
                PRQABuildUtils.normalizeWithEnv(
                        qaFrameworkPostBuildActionSetup.customLicenseServerAddress, run,
                        workspace, listener),
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.qaProject,
                        run, workspace, listener),
                qaFrameworkPostBuildActionSetup.downloadUnifiedProjectDefinition,
                PRQABuildUtils.normalizeWithEnv(
                        qaFrameworkPostBuildActionSetup.unifiedProjectName, run,
                        workspace, listener), qaFrameworkPostBuildActionSetup.enableDependencyMode,
                qaFrameworkPostBuildActionSetup.performCrossModuleAnalysis,
                PRQABuildUtils.normalizeWithEnv(
                        qaFrameworkPostBuildActionSetup.cmaProjectName, run, workspace, listener),
                qaFrameworkPostBuildActionSetup.reuseCmaDb,
                qaFrameworkPostBuildActionSetup.useDiskStorage,
                qaFrameworkPostBuildActionSetup.generateReport,
                qaFrameworkPostBuildActionSetup.publishToQAV,
                qaFrameworkPostBuildActionSetup.loginToQAV,
                qaFrameworkPostBuildActionSetup.uploadWhenStable,
                PRQABuildUtils.normalizeWithEnv(
                        qaFrameworkPostBuildActionSetup.qaVerifyProjectName, run,
                        workspace, listener), PRQABuildUtils.normalizeWithEnv(
                qaFrameworkPostBuildActionSetup.uploadSnapshotName, run, workspace, listener),
                Integer.toString(run.getNumber()),
                qaFrameworkPostBuildActionSetup.uploadSourceCode,
                qaFrameworkPostBuildActionSetup.generateCrr,
                qaFrameworkPostBuildActionSetup.generateMdr,
                qaFrameworkPostBuildActionSetup.generateHis,
                qaFrameworkPostBuildActionSetup.generateSup,
                qaFrameworkPostBuildActionSetup.analysisSettings,
                qaFrameworkPostBuildActionSetup.stopWhenFail,
                qaFrameworkPostBuildActionSetup.customCpuThreads,
                qaFrameworkPostBuildActionSetup.maxNumThreads,
                qaFrameworkPostBuildActionSetup.generatePreprocess,
                qaFrameworkPostBuildActionSetup.assembleSupportAnalytics,
                qaFrameworkPostBuildActionSetup.generateReportOnAnalysisError,
                qaFrameworkPostBuildActionSetup.addBuildNumber,
                qaFrameworkPostBuildActionSetup.projectConfiguration);
    }

    // Function to pull details from QAV Configuration.
    private QAVerifyServerSettings setQaVerifyServerSettings(String configurationByName) {

        QAVerifyServerConfiguration qaVerifyServerConfiguration = PRQAGlobalConfig.get()
                .getConfigurationByName(
                        configurationByName);

        if (qaVerifyServerConfiguration != null) {
            return new QAVerifyServerSettings(qaVerifyServerConfiguration.getHostName(),
                    qaVerifyServerConfiguration.getViewerPortNumber(),
                    qaVerifyServerConfiguration.getProtocol(),
                    qaVerifyServerConfiguration.getPassword(),
                    qaVerifyServerConfiguration.getUserName());
        }

        return new QAVerifyServerSettings();

    }

    private PRQAComplianceStatus performBuild(Run<?, ?> run,
                                              PRQARemoteToolCheck remoteToolCheck,
                                              QAFrameworkRemoteReport remoteReport,
                                              QaFrameworkReportSettings qaReportSettings,
                                              FilePath workspace,
                                              TaskListener listener)
            throws PrqaException, IOException {

        PRQAComplianceStatus currentBuild;

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(workspace.act(remoteToolCheck));
            if (!isQafVersionSupported(qaFrameworkVersion)) {
                throw new PrqaException("Build failure. Please upgrade to a newer version of Helix QAC Framework");
            }
            currentBuild = workspace.act(remoteReport);

            currentBuild.setMessagesWithinThresholdForEachMessageGroup(getThresholdLevel());
            copyArtifacts(run, workspace, qaReportSettings, listener);
        } catch (IOException | InterruptedException ex) {
            outStream.println(Messages.PRQANotifier_FailedGettingResults());
            throw new PrqaException(ex);
        }
        return currentBuild;
    }

    public int getThresholdLevel() {
        if (thresholdsDesc != null) {
            for (AbstractThreshold abstractThreshold : thresholdsDesc) {
                if (abstractThreshold instanceof MessageComplianceThreshold) {
                    return ((MessageComplianceThreshold) abstractThreshold).thresholdLevel;
                }
            }
        }
        return 0;
    }


    private void performUpload(Run<?, ?> build,
                               FilePath workspace,
                               PRQARemoteToolCheck remoteToolCheck,
                               QAFrameworkRemoteReportUpload remoteReportUpload)
            throws PrqaException, IOException {

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(workspace.act(remoteToolCheck));
            if (!isQafVersionSupported(qaFrameworkVersion)) {
                throw new PrqaException("Build failure. Please upgrade to a newer version of Helix QAC Framework");
            }
            workspace.act(remoteReportUpload);
        } catch (IOException | InterruptedException ex) {
            throw new PrqaException(ex);
        }
    }

    private boolean isQafVersionSupported(QaFrameworkVersion qaFrameworkVersion) {
        outStream.println(String.format("Helix QAC Source Code Analysis Framework version %s",
                qaFrameworkVersion.getQaFrameworkVersion()));

        if (!qaFrameworkVersion.isVersionSupported()) {
            outStream.println(String.format(
                    "ERROR: In order to use the Helix QAC plugin please install a version of Helix QAC Framework greater or equal to %s!",
                    MINIMUM_SUPPORTED_VERSION));
            return false;
        }
        return true;
    }

    /*
     * TODO - in Master Salve Setup copy artifacts method do not work and throw exception.
     * This method need to be expanded or suggest user to use Copy Artifact Plugin.
     */
    private void copyArtifacts(Run<?, ?> run,
                               FilePath workspace,
                               QaFrameworkReportSettings qaReportSettings,
                               TaskListener listener) {

        try {
            copyReportsToArtifactsDir(qaReportSettings, run, workspace, listener);
            if (qaReportSettings.isPublishToQAV() && qaReportSettings.isLoginToQAV()) {
                copyResourcesToArtifactsDir("*.log", run, workspace, listener);
            }
        } catch (IOException | InterruptedException ex) {
            log.log(SEVERE, "Failed copying build artifacts from slave to server - Use Copy Artifact Plugin", ex);
            outStream.println("Auto Copy of Build Artifacts to artifact dir on Master Failed");
            outStream.println("Manually add Build Artifacts to artifact dir or use Copy Artifact Plugin ");
            outStream.println(ex.getMessage());
        }
    }
}
