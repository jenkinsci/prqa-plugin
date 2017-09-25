package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
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
import net.praqma.util.structure.Tuple;
import net.prqma.prqa.qaframework.QaFrameworkReportSettings;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

public class PRQANotifier extends Recorder
        implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(PRQANotifier.class.getName());
    private transient PrintStream outStream;
    private List<PRQAGraph> graphTypes;
    public final String settingProjectCompliance;
    public final String snapshotName;

    public QAFrameworkPostBuildActionSetup sourceQAFramework;

    public final List<AbstractThreshold> thresholdsDesc;

    public final String product;
    public final boolean runWhenSuccess;

    public EnumSet<QARReportType> chosenReportTypes;

    @DataBoundConstructor
    public PRQANotifier(
            final String product, final boolean runWhenSuccess, final String settingProjectCompliance,
            final String snapshotName, final QAFrameworkPostBuildActionSetup sourceQAFramework,
            final List<AbstractThreshold> thresholdsDesc) {

        this.product = product;
        this.runWhenSuccess = runWhenSuccess;
        this.settingProjectCompliance = settingProjectCompliance;
        this.snapshotName = snapshotName;

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

    private void copyResourcesToArtifactsDir(String pattern, AbstractBuild<?, ?> build, BuildListener listener) throws IOException,
                                                                                              InterruptedException {
        FilePath buildWorkspace = build.getWorkspace();

        if (buildWorkspace == null) {
            return;
        }

        FilePath[] files = buildWorkspace.list("**/" + pattern);

        Map<String, String> artifacts = new HashMap<>();

        for (FilePath file : files) {
            artifacts.put(file.getName(), StringUtils.replace(file.getRemote(), buildWorkspace.getRemote(), ""));
        }

        if (artifacts.isEmpty()) {
            return;
        }

        BuildListenerAdapter adapter = new BuildListenerAdapter(listener);
        ArtifactManager artifactManager = build.pickArtifactManager();
        Launcher launcher = buildWorkspace.createLauncher(adapter);

        artifactManager.archive(buildWorkspace, launcher, adapter, artifacts);
    }

    /**
     * Process the results
     */
    private boolean evaluate(PRQAReading previousStableComplianceStatus, List<? extends AbstractThreshold> thresholds,
                             PRQAComplianceStatus currentComplianceStatus) {
        PRQAComplianceStatus previousComplianceStatus = (PRQAComplianceStatus) previousStableComplianceStatus;
        HashMap<StatusCategory, Number> tholds = new HashMap<>();
        if (thresholds == null) {
            return true;
        }
        for (AbstractThreshold threshold : thresholds) {
            if (threshold.improvement) {
                if (!isBuildStableForContinuousImprovement(threshold, currentComplianceStatus, previousComplianceStatus)) {
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

    private boolean isBuildStableForContinuousImprovement(AbstractThreshold threshold,
                                                          PRQAComplianceStatus currentComplianceStatus, PRQAComplianceStatus previousComplianceStatus) {
        if (threshold instanceof MessageComplianceThreshold) {
            if (currentComplianceStatus.getMessages() > previousComplianceStatus.getMessages()) {
                currentComplianceStatus.addNotification(Messages
                        .PRQANotifier_MaxMessagesContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getMessages(), currentComplianceStatus.getMessages()));
                return false;
            }
        } else if (threshold instanceof FileComplianceThreshold) {
            if (currentComplianceStatus.getFileCompliance() < previousComplianceStatus.getFileCompliance()) {
                currentComplianceStatus.addNotification(Messages
                        .PRQANotifier_FileComplianceContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getFileCompliance() + "%",
                                currentComplianceStatus.getFileCompliance())
                        + "%");
                return false;
            }
        } else if (threshold instanceof ProjectComplianceThreshold) {
            if (currentComplianceStatus.getProjectCompliance() < previousComplianceStatus.getProjectCompliance()) {
                currentComplianceStatus.addNotification(Messages
                        .PRQANotifier_ProjectComplianceContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getProjectCompliance() + "%",
                                currentComplianceStatus.getProjectCompliance())
                        + "%");
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
    private void addThreshold(AbstractThreshold threshold, HashMap<StatusCategory, Number> tholds) {
        if (threshold instanceof ProjectComplianceThreshold) {
            tholds.put(StatusCategory.ProjectCompliance, ((ProjectComplianceThreshold) threshold).value);
        } else if (threshold instanceof FileComplianceThreshold) {
            tholds.put(StatusCategory.FileCompliance, ((FileComplianceThreshold) threshold).value);
        } else {
            tholds.put(StatusCategory.Messages, ((MessageComplianceThreshold) threshold).value);
        }
    }

    private void copyReportsToArtifactsDir(ReportSettings settings, AbstractBuild<?, ?> build, BuildListener listener) throws IOException,
            InterruptedException {
        FilePath buildWorkspace = build.getWorkspace();

        if (buildWorkspace == null) {
            throw new IOException("Invalid workspace");
        }

        if (settings instanceof QaFrameworkReportSettings) {

            QaFrameworkReportSettings qaFrameworkSettings = (QaFrameworkReportSettings) settings;

            copyGeneratedReportsToJobWorkspace(build,
                                               qaFrameworkSettings.getQaProject(),
                                               qaFrameworkSettings.getProjectConfiguration());
            copyReportsFromWorkspaceToArtifactsDir(build, listener);
        }
    }

    private void copyGeneratedReportsToJobWorkspace(AbstractBuild<?, ?> build,
                                                    final String qaProject,
                                                    String projectConfiguration)
            throws
            IOException,
            InterruptedException {

        FilePath buildWorkspace = build.getWorkspace();

        if (buildWorkspace == null) {
            throw new IOException("Invalid workspace");
        }

        buildWorkspace.act(new CopyReportsToWorkspace(qaProject,
                                                      projectConfiguration));
    }

    private boolean containsReportName(String fileName) {
        return fileName.contains(CRR.name()) ||
                fileName.contains(SUR.name()) ||
                fileName.contains(RCR.name()) ||
                fileName.contains(MDR.name());
    }

    private void copyReportsFromWorkspaceToArtifactsDir(AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException, InterruptedException {

        // COPY only last generated reports
        FilePath buildWorkspace = build.getWorkspace();
        if (buildWorkspace == null) {
            throw new IOException("Invalid workspace");
        }

        List<FilePath> workspaceFiles = buildWorkspace.list();
        if (workspaceFiles.isEmpty()) {
            return;
        }

        Collections.sort(workspaceFiles, new Comparator<FilePath>() {
            @Override
            public int compare(FilePath o1, FilePath o2) {
                try {
                    return Long.compare(o2.lastModified(), o1.lastModified());
                } catch (IOException | InterruptedException e) {
                    return 0;
                }
            }
        });

        Map<String, String> artifacts = new HashMap<>();

        for (FilePath file : workspaceFiles) {
            if (file.lastModified() < build.getTimeInMillis()) {
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
        Launcher launcher = buildWorkspace.createLauncher(adapter);
        ArtifactManager artifactManager = build.pickArtifactManager();

        artifactManager.archive(buildWorkspace, launcher, adapter, artifacts);

    }

    public List<PRQAGraph> getSupportedGraphs() {
        ArrayList<PRQAGraph> graphs = new ArrayList<>();
        for (PRQAGraph g : graphTypes) {
            if (g.getType().equals(QARReportType.Compliance)) {
                graphs.add(g);
            }
        }
        return graphs;
    }

    public PRQAGraph getGraph(String simpleClassName) {
        for (PRQAGraph p : getSupportedGraphs()) {
            if (p.getClass().getSimpleName().equals(simpleClassName)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Pre-build for the plugin. We use this one to clean up old report files.
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        boolean success = false;
        FilePath workspace = build.getWorkspace();

        if (workspace == null) {
            listener.getLogger().println("Invalid workspace. Cannot continue.");
            return false;
        }

        DeleteReportsFromWorkspace deleter = new DeleteReportsFromWorkspace();

        try {
            success = workspace.act(deleter);
        } catch (IOException | InterruptedException ex) {
            log.log(SEVERE, "Cleanup crew missing!", ex);
            listener.getLogger().println(ex.getMessage());
        }

        if (!success) {
            listener.getLogger().println("Failed to cleanup workspace reports.");
            build.setResult(FAILURE);
        }
        return success;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        outStream = listener.getLogger();

        Result buildResult = build.getResult();

        if (buildResult == null) {
            log.log(WARNING, "Build result is unavailable at this point.");
            outStream.println("Build result is unavailable at this point. Will not proceed.");
            return false;
        } else if (buildResult.isWorseOrEqualTo(FAILURE)){
            log.log(WARNING, "Build is marked as failure.");
            if (runWhenSuccess) {
                outStream.println("Build is marked as failure and PRQA Analysis will not proceed.");
                return false;
            }
            outStream.println("Build is marked as failure but PRQA Analysis will proceed.");
        }

        return performQaFrameworkBuild(build, launcher, listener);
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
    private Tuple<PRQAReading, AbstractBuild<?, ?>> getPreviousReading(AbstractBuild<?, ?> currentBuild,
                                                                       Result expectedResult) {
        AbstractBuild<?, ?> iterate = currentBuild;
        do {
            iterate = iterate.getPreviousNotFailedBuild();
            if (iterate != null && iterate.getAction(PRQABuildAction.class) != null
                    && Objects.equals(iterate.getResult(), expectedResult)) {
                Tuple<PRQAReading, AbstractBuild<?, ?>> result = new Tuple<>();
                result.setSecond(iterate);
                result.setFirst(iterate.getAction(PRQABuildAction.class).getResult());
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
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public List<ThresholdSelectionDescriptor<?>> getThresholdSelections() {
            return AbstractThreshold.getDescriptors();
        }

        @Override
        public String getDisplayName() {
            return "Programming Research Report";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, @Nonnull JSONObject formData) throws Descriptor.FormException {

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
            if (CollectionUtils.isEmpty(instance.getGraphTypes())) {
                ArrayList<PRQAGraph> list = new ArrayList<>();
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
                instance.setGraphTypes(list);
            }

            save();
            return instance;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            save();
            return super.configure(req, json);
        }

        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }

        public List<QAFrameworkInstallationConfiguration> getQaFrameworkTools() {

            Jenkins jenkins = Jenkins.getInstance();

            if (jenkins == null) {
                throw new RuntimeException("Unable to get Jenkins instance");
            }

            QAFrameworkInstallationConfiguration[] prqaInstallations = jenkins
                    .getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }

        public List<PRQAReportSourceDescriptor<?>> getReportSources() {
            return PostBuildActionSetup.getDescriptors();
        }

        public PRQAReportSourceDescriptor<?> getReportSource() {
            return PostBuildActionSetup.getDescriptors().get(0);
        }
    }

    private boolean performQaFrameworkBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        FilePath workspace = build.getWorkspace();

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        Computer currentComputer = Computer.currentComputer();

        if (currentComputer == null) {
            throw new IOException("Invalid machine. Cannot continue.");
        }

        Node node = currentComputer.getNode();
        if (node == null) {
            throw new IOException("Invalid machine. Cannot continue.");
        }

        QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup = sourceQAFramework;
        QAFrameworkInstallationConfiguration qaFrameworkInstallationConfiguration = QAFrameworkInstallationConfiguration
                .getInstallationByName(qaFrameworkPostBuildActionSetup.qaInstallation);

        if (qaFrameworkInstallationConfiguration == null) {
            String msg = String.format(
                    "The job uses a QA Framework installation (%s) that is misconfigured or no longer exists, please reconfigure.",
                    qaFrameworkPostBuildActionSetup.qaInstallation);
            log.log(SEVERE, msg);
            outStream.println(msg);
            build.setResult(FAILURE);
            return false;
        }

        qaFrameworkInstallationConfiguration = qaFrameworkInstallationConfiguration.forNode(node, listener);

        outStream.println(String.format("Programming Research Quality Assurance Plugin version %s", VersionInfo.getPluginVersion()));

        PRQAToolSuite suite = qaFrameworkInstallationConfiguration;

        outStream.println(Messages.PRQANotifier_ReportGenerateText());
        outStream.println("Workspace : " + workspace.getRemote());

        HashMap<String, String> environmentVariables;

        environmentVariables = suite.createEnvironmentVariables(workspace.getRemote());

        PRQAApplicationSettings appSettings = new PRQAApplicationSettings(
                qaFrameworkInstallationConfiguration.getHome());
        QaFrameworkReportSettings qaReportSettings;
        try {
            qaReportSettings = setQaFrameworkReportSettings(qaFrameworkPostBuildActionSetup, build, listener);
        } catch (PrqaSetupException ex) {
            log.log(SEVERE, ex.getMessage(), ex);
            outStream.println(ex.getMessage());
            build.setResult(FAILURE);
            return false;
        }

        Collection<QAFrameworkRemoteReport> remoteReports = new ArrayList<>();

        Collection<QAFrameworkRemoteReportUpload> remoteReportUploads = new ArrayList<>();

        if (qaFrameworkPostBuildActionSetup.chosenServers != null &&
                !qaFrameworkPostBuildActionSetup.chosenServers.isEmpty()) {
            for (String chosenServer : qaFrameworkPostBuildActionSetup.chosenServers) {
                QAVerifyServerSettings qaVerifySettings = setQaVerifyServerSettings(chosenServer);
                QAFrameworkReport report = new QAFrameworkReport(qaReportSettings,
                                                                 qaVerifySettings,
                                                                 environmentVariables);

                remoteReports.add(new QAFrameworkRemoteReport(report, listener, launcher.isUnix()));
                remoteReportUploads.add(new QAFrameworkRemoteReportUpload(report, listener));
            }
        } else {
            QAFrameworkReport report = new QAFrameworkReport(qaReportSettings,
                                                             setQaVerifyServerSettings(null),
                                                             environmentVariables);
            remoteReports.add(new QAFrameworkRemoteReport(report, listener, launcher.isUnix()));
        }

        PRQARemoteToolCheck remoteToolCheck = new PRQARemoteToolCheck(new QACli(), environmentVariables, appSettings,
                qaReportSettings, listener, launcher.isUnix());
        PRQAComplianceStatus currentBuild;
        try {
            QAFrameworkRemoteReport remoteReport = remoteReports.iterator().next();

            currentBuild = performBuild(build, remoteToolCheck, remoteReport, qaReportSettings, listener);
        } catch (PrqaException ex) {
            log.log(SEVERE, ex.getMessage(), ex);
            outStream.println(ex.getMessage());
            build.setResult(FAILURE);
            return false;
        }

        Tuple<PRQAReading, AbstractBuild<?, ?>> previousBuildResultTuple = getPreviousReading(build, SUCCESS);

        PRQAReading previousStableBuildResult = previousBuildResultTuple != null ? previousBuildResultTuple.getFirst()
                : null;

        log.fine("thresholdsDesc is null: " + (thresholdsDesc == null));
        if (thresholdsDesc != null) {
            log.fine("thresholdsDescSize: " + thresholdsDesc.size());
        }

        boolean thresholdEvalResult = evaluate(previousStableBuildResult, thresholdsDesc, currentBuild);
        log.fine("Evaluated to: " + thresholdEvalResult);

        Result buildResult = build.getResult();

        if (buildResult == null) {
            log.log(WARNING, "Build result is unavailable at this point.");
            outStream.println("Build result is unavailable at this point. Will not proceed.");
            return false;
        }
        if (!thresholdEvalResult && !buildResult.isWorseOrEqualTo(FAILURE)) {
            build.setResult(UNSTABLE);
        }

        if (qaReportSettings.isLoginToQAV() && qaReportSettings.isPublishToQAV()) {
            if (qaReportSettings.isQaUploadWhenStable() && buildResult.isWorseOrEqualTo(UNSTABLE)) {
                outStream.println("Upload warning: QAV Upload cant be perform because build is Unstable");
                log.log(WARNING, "UPLOAD WARNING - QAV Upload cant be perform because build is Unstable");
            } else {
                if (buildResult.isWorseOrEqualTo(UNSTABLE)) {
                    outStream.println("Upload warning: Build is Unstable but upload will continue...");
                }
                outStream.println("Upload info: QAV Upload...");
                for (QAFrameworkRemoteReportUpload remoteReportUpload : remoteReportUploads) {
                    try {
                        performUpload(build, remoteToolCheck, remoteReportUpload);
                    } catch (PrqaException ex) {
                        log.log(SEVERE, ex.getMessage(), ex);
                        outStream.println(ex.getMessage());
                        build.setResult(FAILURE);
                        return false;
                    }
                }
            }
        }

        outStream.println("\n----------------------BUILD Results-----------------------\n");
        if (previousBuildResultTuple != null) {
            outStream.println(Messages.PRQANotifier_PreviousResultBuildNumber(previousBuildResultTuple.getSecond().number));
            outStream.println(previousBuildResultTuple.getFirst());

        } else {
            outStream.println(Messages.PRQANotifier_NoPreviousResults());
        }
        outStream.println(Messages.PRQANotifier_ScannedValues());
        outStream.println(currentBuild);

        PRQABuildAction action = new PRQABuildAction(build);
        action.setResult(currentBuild);
        action.setPublisher(this);
        build.addAction(action);
        return true;
    }

    private QaFrameworkReportSettings setQaFrameworkReportSettings(
            QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup, AbstractBuild<?, ?> build, BuildListener listener)
            throws PrqaSetupException {

        if (qaFrameworkPostBuildActionSetup.qaProject == null) {
            throw new PrqaSetupException("Project configuration is missing. Please set a project in Qa Framework configuration section!");
        }

        return new QaFrameworkReportSettings(
                qaFrameworkPostBuildActionSetup.qaInstallation,
                qaFrameworkPostBuildActionSetup.useCustomLicenseServer,
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.customLicenseServerAddress, build, listener),
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.qaProject, build, listener),
                qaFrameworkPostBuildActionSetup.downloadUnifiedProjectDefinition,
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.unifiedProjectName, build, listener),
                qaFrameworkPostBuildActionSetup.enableDependencyMode,
                qaFrameworkPostBuildActionSetup.performCrossModuleAnalysis,
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.cmaProjectName, build, listener),
                qaFrameworkPostBuildActionSetup.reuseCmaDb,
                qaFrameworkPostBuildActionSetup.useDiskStorage,
                qaFrameworkPostBuildActionSetup.generateReport,
                qaFrameworkPostBuildActionSetup.publishToQAV,
                qaFrameworkPostBuildActionSetup.loginToQAV,
                product,
                qaFrameworkPostBuildActionSetup.uploadWhenStable,
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.qaVerifyProjectName, build, listener),
                PRQABuildUtils.normalizeWithEnv(qaFrameworkPostBuildActionSetup.uploadSnapshotName, build, listener),
                Integer.toString(build.getNumber()),
                qaFrameworkPostBuildActionSetup.uploadSourceCode,
                qaFrameworkPostBuildActionSetup.generateCrr,
                qaFrameworkPostBuildActionSetup.generateMdr,
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

        QAVerifyServerConfiguration qaVerifyServerConfiguration = PRQAGlobalConfig.get().getConfigurationByName(
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

    private PRQAComplianceStatus performBuild(AbstractBuild<?, ?> build,
                                              PRQARemoteToolCheck remoteToolCheck, QAFrameworkRemoteReport remoteReport,
                                              QaFrameworkReportSettings qaReportSettings, BuildListener listener) throws PrqaException, IOException {

        PRQAComplianceStatus currentBuild;
        FilePath workspace = build.getWorkspace();

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(workspace.act(remoteToolCheck));
            if (!isQafVersionSupported(qaFrameworkVersion)) {
                throw new PrqaException("Build failure. Please upgrade to a newer version of PRQA Framework");
            }
            remoteReport.setQaFrameworkVersion(qaFrameworkVersion);
            currentBuild = workspace.act(remoteReport);
            
            currentBuild.setMessagesWithinThresholdForEachMessageGroup(getThresholdLevel());
            copyArtifacts(build, qaReportSettings, listener);
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


    private void performUpload(AbstractBuild<?, ?> build, PRQARemoteToolCheck remoteToolCheck,
                               QAFrameworkRemoteReportUpload remoteReportUpload) throws PrqaException, IOException {


        FilePath workspace = build.getWorkspace();

        if (workspace == null) {
            throw new IOException("Invalid workspace. Cannot continue.");
        }

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(workspace.act(remoteToolCheck));
            if (!isQafVersionSupported(qaFrameworkVersion)) {
                throw new PrqaException("Build failure. Please upgrade to a newer version of PRQA Framework");
            }
            remoteReportUpload.setQaFrameworkVersion(qaFrameworkVersion);
            workspace.act(remoteReportUpload);
        } catch (IOException | InterruptedException ex) {
            throw new PrqaException(ex);
        }
    }

    private boolean isQafVersionSupported(QaFrameworkVersion qaFrameworkVersion) {
        outStream.println(String.format("PRQA Source Code Analysis Framework version %s",
                qaFrameworkVersion.getQaFrameworkVersion()));

        if (!qaFrameworkVersion.isVersionSupported()) {
            outStream.println(String.format(
                    "ERROR: In order to use the PRQA plugin please install a version of PRQA·Framework greater or equal to %s!",
                    MINIMUM_SUPPORTED_VERSION));
            return false;
        }
        return true;
    }

    /*
     * TODO - in Master Salve Setup copy artifacts method do not work and throw exception.
     * This method need to be expanded or suggest user to use Copy Artifact Plugin.
     */
    private void copyArtifacts(AbstractBuild<?, ?> build, QaFrameworkReportSettings qaReportSettings, BuildListener listener) {

        try {
            copyReportsToArtifactsDir(qaReportSettings, build, listener);
            if (qaReportSettings.isPublishToQAV() && qaReportSettings.isLoginToQAV()) {
                copyResourcesToArtifactsDir("*.log", build, listener);
            }
        } catch (IOException | InterruptedException ex) {
            log.log(SEVERE, "Failed copying build artifacts from slave to server - Use Copy Artifact Plugin", ex);
            outStream.println("Auto Copy of Build Artifacts to artifact dir on Master Failed");
            outStream.println("Manually add Build Artifacts to artifact dir or use Copy Artifact Plugin ");
            outStream.println(ex.getMessage());
        }
    }
}
