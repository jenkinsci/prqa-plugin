/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.jenkins.plugin.prqa.PRQARemoteReport;
import net.praqma.jenkins.plugin.prqa.PRQARemoteToolCheck;
import net.praqma.jenkins.plugin.prqa.QAFrameworkRemoteReport;
import net.praqma.jenkins.plugin.prqa.QAFrameworkRemoteReportUpload;
import net.praqma.jenkins.plugin.prqa.VersionInfo;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.graphs.ComplianceIndexGraphs;
import net.praqma.jenkins.plugin.prqa.graphs.MessagesGraph;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.jenkins.plugin.prqa.setup.PRQAToolSuite;
import net.praqma.jenkins.plugin.prqa.setup.QACToolSuite;
import net.praqma.jenkins.plugin.prqa.setup.QAFrameworkInstallationConfiguration;
import net.praqma.jenkins.plugin.prqa.threshold.AbstractThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.FileComplianceThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.MessageComplianceThreshold;
import net.praqma.jenkins.plugin.prqa.threshold.ProjectComplianceThreshold;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAReportSettings;
import net.praqma.prqa.PRQAToolUploadSettings;
import net.praqma.prqa.QAVerifyServerSettings;
import net.praqma.prqa.QaFrameworkVersion;
import net.praqma.prqa.ReportSettings;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaParserException;
import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.exceptions.PrqaUploadException;
import net.praqma.prqa.products.QAC;
import net.praqma.prqa.products.QACli;
import net.praqma.prqa.products.QACpp;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.products.QAW;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.reports.QAFrameworkReport;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.StatusCategory;
import net.praqma.util.ExceptionUtils;
import net.praqma.util.structure.Tuple;
import net.prqma.prqa.qaframework.QaFrameworkReportSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.google.common.base.Strings;

//TODO: I intend to REMOVE all the deprecated fields in the realease for the new PRQA API
public class PRQANotifier extends Publisher {

    private static final Logger log = Logger.getLogger(PRQANotifier.class.getName());
    private PrintStream outStream;
    private QaFrameworkVersion qaFrameworkVersion;
    private List<PRQAGraph> graphTypes;
    public final String settingProjectCompliance;
    public final String snapshotName;

    public final PostBuildActionSetup sourceQAFramework;
    public final PRQAFileProjectSource sourceFileProject;
    public final int threshholdlevel;
    public final List<AbstractThreshold> thresholdsDesc;

    public final String product;
    public final boolean runWhenSuccess;

    public EnumSet<QARReportType> chosenReportTypes;

    @SuppressWarnings("deprecation")
    @DataBoundConstructor
    public PRQANotifier(
            final String product, final boolean runWhenSuccess, final String settingProjectCompliance,
            final String snapshotName, final int threshholdlevel, final PostBuildActionSetup sourceQAFramework,
            final PRQAFileProjectSource sourceFileProject, final List<AbstractThreshold> thresholdsDesc) {

        this.product = product;
        this.runWhenSuccess = runWhenSuccess;
        this.settingProjectCompliance = settingProjectCompliance;
        this.snapshotName = snapshotName;

        this.sourceQAFramework = sourceQAFramework;
        this.sourceFileProject = sourceFileProject;
        this.threshholdlevel = threshholdlevel;
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

    private void copyReourcesToArtifactsDir(String pattern, AbstractBuild<?, ?> build) throws IOException,
            InterruptedException {
        FilePath[] files = build.getWorkspace().list("**/" + pattern);
        if (files.length >= 1) {
            for (int i = 0; i < files.length; i++) {
                String artifactDir = build.getArtifactsDir().getPath();
                FilePath targetDir = new FilePath(new File(artifactDir + "/" + files[i].getName()));
                files[i].copyTo(targetDir);
                outStream.println(Messages.PRQANotifier_SuccesFileCopy(files[i].getName()));
            }
        }
    }

    /**
     * Process the results
     */
    private boolean evaluate(PRQAReading previousStabileComplianceStatus, List<? extends AbstractThreshold> thresholds,
            PRQAComplianceStatus currentComplianceStatus) {
        PRQAComplianceStatus previousComplianceStatus = (PRQAComplianceStatus) previousStabileComplianceStatus;
        HashMap<StatusCategory, Number> tholds = new HashMap<StatusCategory, Number>();
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
                if (!threshold.validate(previousComplianceStatus, currentComplianceStatus, threshholdlevel)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBuildStableForContinuousImprovement(AbstractThreshold treshold,
            PRQAComplianceStatus currentComplianceStatus, PRQAComplianceStatus previousComplianceStatus) {
        if (treshold instanceof MessageComplianceThreshold) {
            if (currentComplianceStatus.getMessages() > previousComplianceStatus.getMessages()) {
                currentComplianceStatus.addNotification(Messages
                        .PRQANotifier_MaxMessagesContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getMessages(), currentComplianceStatus.getMessages()));
                return false;
            }
        } else if (treshold instanceof FileComplianceThreshold) {
            if (currentComplianceStatus.getFileCompliance() < previousComplianceStatus.getFileCompliance()) {
                currentComplianceStatus.addNotification(Messages
                        .PRQANotifier_FileComplianceContinuousImprovementRequirementNotMet(
                                previousComplianceStatus.getFileCompliance() + "%",
                                currentComplianceStatus.getFileCompliance())
                        + "%");
                return false;
            }
        } else if (treshold instanceof ProjectComplianceThreshold) {
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

    private void copyReportsToArtifactsDir(ReportSettings settings, AbstractBuild<?, ?> build) throws IOException,
            InterruptedException {
        if (settings instanceof PRQAReportSettings) {
            PRQAReportSettings prqaReportSettings = (PRQAReportSettings) settings;
            for (PRQAContext.QARReportType type : prqaReportSettings.chosenReportTypes) {
                String pattern = "**/" + PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION);
                FilePath[] files = build.getWorkspace().list(pattern);
                if (files.length >= 1) {
                    outStream.println(Messages.PRQANotifier_FoundReport(PRQAReport.getNamingTemplate(type,
                            PRQAReport.XHTML_REPORT_EXTENSION)));
                    String artifactDir = build.getArtifactsDir().getPath();

                    FilePath targetDir = new FilePath(new File(artifactDir + "/"
                            + PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION)));
                    outStream.println(Messages.PRQANotifier_CopyToTarget(targetDir.getName()));

                    build.getWorkspace().list(
                            "**/" + PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION))[0]
                            .copyTo(targetDir);
                    outStream.println(Messages.PRQANotifier_SuccesCopyReport());
                }
            }
        } else if (settings instanceof QaFrameworkReportSettings) {

            QaFrameworkReportSettings qaFrameworkSettings = (QaFrameworkReportSettings) settings;
            File workspace = new File(build.getWorkspace().toURI().getPath());

            File artefact = build.getArtifactsDir();
            try {
                copyGeneratedReportsToJobWorkspace(workspace, qaFrameworkSettings.getQaProject());
                copyReportsFromWorkspaceToArtefactsDir(artefact, workspace, build.getTimeInMillis());
            } catch (IOException ex) {

            }
        }
    }

    private void copyGeneratedReportsToJobWorkspace(File workspace, String qaProject) throws IOException {
        if (workspace == null || qaProject == null) {
            return;
        }
        String reportsPath = "prqa/reports";
        File qaFReports = new File(qaProject + "/" + reportsPath);

        if (!qaFReports.isDirectory()) {
            qaFReports = new File(workspace + "/" + qaProject + "/" + reportsPath);
        }

        if (qaFReports.isDirectory()) {
            File[] files = qaFReports.listFiles();
            if (files.length < 1) {
                return;
            }
            if (workspace.isDirectory()) {
                for (File reportFile : files) {
                    if (reportFile.getName().contains("RCR") || reportFile.getName().contains("SUR") || reportFile.getName().contains("CRR")
                            || reportFile.getName().contains("MDR")) {
                        FileUtils.copyFileToDirectory(reportFile, workspace);
                    }
                }
            }
        }
    }

    private void copyReportsFromWorkspaceToArtefactsDir(File artefact, File workspace, long elapsedTime)
            throws IOException {
        if (artefact == null) {
            return;
        }
        if (!artefact.exists()) {
            artefact.mkdirs();
        }
        // clean artefacts dir
        FileUtils.cleanDirectory(artefact);
        // COPY only last generated reports
        File[] workspaceFiles = workspace.listFiles();
        if (workspaceFiles.length < 1) {
            return;
        }
        Arrays.sort(workspaceFiles, new Comparator<File>() {

            @Override
            public int compare(File o1, File o2) {
                if (o1.lastModified() > o2.lastModified()) {
                    return -1;
                } else if (o1.lastModified() < o2.lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
            }
        });

        for (File file : workspaceFiles) {
            if (file.lastModified() < elapsedTime) {
                break;
            }
            if (file.getName().contains("CRR")) {
                FileUtils.copyFileToDirectory(file, artefact);
            }
            if (file.getName().contains("SUR")) {
                FileUtils.copyFileToDirectory(file, artefact);
            }
            if (file.getName().contains("RCR")) {
                FileUtils.copyFileToDirectory(file, artefact);
            }
            if (file.getName().contains("MDR")) {
                FileUtils.copyFileToDirectory(file, artefact);
            }
        }
    }

    public List<PRQAGraph> getSupportedGraphs() {
        ArrayList<PRQAGraph> graphs = new ArrayList<PRQAGraph>();
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

    public PRQAGraph getGraph(Class clazz, List<PRQAGraph> graphs) {
        for (PRQAGraph p : graphs) {
            if (p.getClass().equals(clazz)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Pre-build for the plugin. We use this one to clean up old report files.
     *
     * @param build
     * @param listener
     * @return
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        try {
            List<FilePath> files = build.getWorkspace().list(new ReportFileFilter());
            int numberOfReportFiles = build.getWorkspace().list(new ReportFileFilter()).size();
            if (numberOfReportFiles > 0) {
                listener.getLogger().println(
                        String.format("Found %s report fragments, cleaning up", numberOfReportFiles));
            }

            int deleteCounter = 0;
            for (FilePath f : files) {
                f.delete();
                deleteCounter++;
            }

            if (deleteCounter > 0) {
                listener.getLogger().println(String.format("Succesfully deleted %s report fragments", deleteCounter));
            }

        } catch (IOException ex) {
            listener.getLogger().println("Failed to clean up stale report files");
            log.log(Level.SEVERE, "Cleanup crew missing!", ex);

        } catch (InterruptedException ex) {
            listener.getLogger().println("Failed to clean up stale report files");
            log.log(Level.SEVERE, "Cleanup crew missing!", ex);
        }

        return true;
    }
    // TODO: Refactor this method is way too long

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Result buildResult = build.getResult();
        if (buildResult.isWorseOrEqualTo(Result.FAILURE) && runWhenSuccess) {
            build.setResult(Result.FAILURE);
            return false;
        }
        outStream = listener.getLogger();
        if (sourceQAFramework != null && sourceQAFramework instanceof QAFrameworkPostBuildActionSetup) {

            performQaFrameworkBuild(build, launcher, listener);

        } else if (sourceQAFramework != null && sourceQAFramework instanceof PRQAReportPRQAToolSource) {

            PRQAReportPRQAToolSource prqaReportPRQAToolSource = (PRQAReportPRQAToolSource) sourceQAFramework;

            String productUsed = prqaReportPRQAToolSource.product;
            outStream = listener.getLogger();

            PRQAToolSuite suite = null;
            QACToolSuite qacSuite = QACToolSuite.getInstallationByName(prqaReportPRQAToolSource.product);

            outStream.println(VersionInfo.getPluginVersion());

            if (qacSuite != null) {
                productUsed = qacSuite.tool;
                suite = qacSuite;
            }

            QAVerifyServerConfiguration conf = PRQAGlobalConfig.get().getConfigurationByName(prqaReportPRQAToolSource.chosenServer);

            PRQAApplicationSettings appSettings = null;
            if (suite != null) {
                if (suite instanceof QACToolSuite) {
                    QACToolSuite cSuite = (QACToolSuite) suite;
                    appSettings = new PRQAApplicationSettings(cSuite.qarHome, cSuite.qavHome, cSuite.qawHome,
                            cSuite.getHome());
                }
            }

            PRQAReportSettings settings = null;
            QAR qar = null;

            if (prqaReportPRQAToolSource.fileProjectSource != null
                    && prqaReportPRQAToolSource.fileProjectSource instanceof PRQAReportProjectFileSource) {
                PRQAReportProjectFileSource pSource = (PRQAReportProjectFileSource) prqaReportPRQAToolSource.fileProjectSource;
                settings = new PRQAReportSettings(prqaReportPRQAToolSource.chosenServer, pSource.projectFile,
                        prqaReportPRQAToolSource.performCrossModuleAnalysis, prqaReportPRQAToolSource.publishToQAV,
                        prqaReportPRQAToolSource.enableDependencyMode, prqaReportPRQAToolSource.enableDataFlowAnalysis,
                        chosenReportTypes, productUsed);
                qar = new QAR(productUsed, pSource.projectFile, QARReportType.Compliance);

            } else if (prqaReportPRQAToolSource.fileProjectSource != null
                    && prqaReportPRQAToolSource.fileProjectSource instanceof PRQAReportFileListSource) {
                PRQAReportFileListSource flSource = (PRQAReportFileListSource) prqaReportPRQAToolSource.fileProjectSource;

                settings = new PRQAReportSettings(prqaReportPRQAToolSource.chosenServer, flSource.fileList,
                        prqaReportPRQAToolSource.performCrossModuleAnalysis, prqaReportPRQAToolSource.publishToQAV,
                        prqaReportPRQAToolSource.enableDependencyMode, prqaReportPRQAToolSource.enableDataFlowAnalysis,
                        chosenReportTypes, productUsed);
                qar = new QAR(productUsed, flSource.fileList, QARReportType.Compliance);

            } else {
                // Use old settings (projectFile ~ Still exists)
                settings = new PRQAReportSettings(prqaReportPRQAToolSource.chosenServer,
                        prqaReportPRQAToolSource.projectFile, prqaReportPRQAToolSource.performCrossModuleAnalysis,
                        prqaReportPRQAToolSource.publishToQAV, prqaReportPRQAToolSource.enableDependencyMode,
                        prqaReportPRQAToolSource.enableDataFlowAnalysis, chosenReportTypes, productUsed);
                qar = new QAR(productUsed, prqaReportPRQAToolSource.projectFile, QARReportType.Compliance);
            }

            outStream.println(Messages.PRQANotifier_ReportGenerateText());
            outStream.println(qar);

            PRQAToolUploadSettings uploadSettings = new PRQAToolUploadSettings(prqaReportPRQAToolSource.vcsConfigXml,
                    prqaReportPRQAToolSource.singleSnapshotMode, prqaReportPRQAToolSource.codeUploadSetting,
                    prqaReportPRQAToolSource.sourceOrigin, prqaReportPRQAToolSource.qaVerifyProjectName);

            QAVerifyServerSettings qavSettings = null;
            if (conf != null) {
                qavSettings = new QAVerifyServerSettings(conf.getHostName(), conf.getViewerPortNumber(), conf.getProtocol(),
                        conf.getPassword(), conf.getUserName());
            }

            HashMap<String, String> environment = null;
            if (suite != null) {
                environment = suite.createEnvironmentVariables(build.getWorkspace().getRemote());
            }
            outStream.println("Workspace : " + build.getWorkspace().getRemote());
            boolean success = true;
            PRQAComplianceStatus currentBuild = null;

            try {

                if (qacSuite == null && !(productUsed.equalsIgnoreCase("qacpp") || productUsed.equalsIgnoreCase("qac"))) {
                    throw new PrqaSetupException(String.format(
                            "The job uses a product configuration (%s) that no longer exists, please reconfigure.",
                            productUsed));
                }

                PRQAReport report = new PRQAReport(settings, qavSettings, uploadSettings, appSettings, environment);
                if (productUsed.equalsIgnoreCase("qac")) {
                    String qacVersion = build.getWorkspace().act(
                            new PRQARemoteToolCheck(new QAC(), environment, appSettings, settings, listener, launcher
                                    .isUnix()));
                    outStream.println("QA·C OK - " + qacVersion);
                } else if (productUsed.equalsIgnoreCase("qacpp")) {
                    String qacppVersion = build.getWorkspace().act(
                            new PRQARemoteToolCheck(new QACpp(), environment, appSettings, settings, listener, launcher
                                    .isUnix()));
                    outStream.println("QA·C++ OK - " + qacppVersion);
                }

                String qawVersion = build.getWorkspace().act(
                        new PRQARemoteToolCheck(new QAW(), environment, appSettings, settings, listener, launcher
                                .isUnix()));
                outStream.println("QAW OK - " + qawVersion);

                String qarVersion = build.getWorkspace().act(
                        new PRQARemoteToolCheck(qar, environment, appSettings, settings, listener, launcher.isUnix()));
                outStream.println("QAR OK - " + qarVersion);

                currentBuild = build.getWorkspace().act(new PRQARemoteReport(report, listener, launcher.isUnix()));
                currentBuild.setMessagesWithinThreshold(currentBuild.getMessageCount(threshholdlevel));
            } catch (IOException ex) {
                success = treatIOException(ex);
                return success;
            } catch (PrqaException pex) {
                outStream.println(pex.getMessage());
                log.log(Level.WARNING, "PrqaException", pex);
                return false;
            } catch (Exception ex) {
                outStream.println(Messages.PRQANotifier_FailedGettingResults());
                outStream.println("This should not be happinging, writing error to log");
                log.log(Level.SEVERE, "Unhandled exception", ex);
                return false;
            } finally {
                try {
                    if (success) {
                        copyReportsToArtifactsDir(settings, build);
                    }
                    if (prqaReportPRQAToolSource.publishToQAV && success) {
                        copyReourcesToArtifactsDir("*.log", build);
                    }
                } catch (Exception ex) {
                    outStream.println("Error in copying artifacts to artifact dir");
                    log.log(Level.SEVERE, "Failed copying build artifacts", ex);
                }
            }

            Tuple<PRQAReading, AbstractBuild<?, ?>> previousBuildResultTuple = getPreviousReading(build, Result.SUCCESS);

            if (previousBuildResultTuple != null) {
                outStream.println(String.format(Messages.PRQANotifier_PreviousResultBuildNumber(new Integer(
                        previousBuildResultTuple.getSecond().number))));
                outStream.println(previousBuildResultTuple.getFirst());
            } else {
                outStream.println(Messages.PRQANotifier_NoPreviousResults());
            }

            PRQAReading previousBuildResult = previousBuildResultTuple != null ? previousBuildResultTuple.getFirst()
                    : null;

            boolean buildStatus = true;

            log.fine("thresholdsDesc is null: " + (thresholdsDesc == null));
            if (thresholdsDesc != null) {
                log.fine("thresholdsDescSize: " + thresholdsDesc.size());
            }

            try {
                buildStatus = evaluate(previousBuildResult, thresholdsDesc, currentBuild);
                log.fine("Evaluated to: " + buildStatus);
            } catch (Exception ex) {
                outStream.println("Report generation ok. Caught exception evaluation results. Trace written to log");
                log.log(Level.SEVERE, "Storing unexpected result evalution exception", ex);
            }

            outStream.println(Messages.PRQANotifier_ScannedValues());
            outStream.println(currentBuild);

            PRQABuildAction action = new PRQABuildAction(build);
            action.setResult(currentBuild);
            action.setPublisher(this);
            if (!buildStatus) {
                build.setResult(Result.UNSTABLE);
            }
            build.getActions().add(action);
            return true;

        }
        return true;
    }

    private boolean treatIOException(IOException ex) {
        Throwable myCase = ExceptionUtils.unpackFrom(IOException.class, ex);

        if (myCase instanceof PrqaSetupException) {
            outStream.println(String.format(
                    "Most likely cause is a misconfigured tool, refer to documentation (%s) on how to configure them.",
                    VersionInfo.WIKI_PAGE));
            outStream.println(myCase.getMessage());
            log.log(Level.SEVERE, "Logging PrqaSetupException", myCase);
        } else if (myCase instanceof PrqaUploadException) {
            outStream.println("Upload failed");
            outStream.println(myCase.getMessage());
            log.log(Level.SEVERE, "Logging PrqaUploadException", myCase);
        } else if (myCase instanceof PrqaParserException) {
            outStream.println(myCase.getMessage());
            log.log(Level.SEVERE, "Logging PrqaException", myCase);
        } else if (myCase instanceof PrqaException) {
            outStream.println(myCase.getMessage());
            log.log(Level.SEVERE, "Logging PrqaException", ex);
        }
        return false;
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
     *
     * @param build
     * @param expectedResult
     * @return
     */
    private Tuple<PRQAReading, AbstractBuild<?, ?>> getPreviousReading(AbstractBuild<?, ?> currentBuild,
            Result expectedResult) {
        Tuple<PRQAReading, AbstractBuild<?, ?>> result = null;
        AbstractBuild<?, ?> iterate = currentBuild;
        do {
            iterate = iterate.getPreviousNotFailedBuild();
            if (iterate != null && iterate.getAction(PRQABuildAction.class) != null
                    && iterate.getResult().equals(expectedResult)) {
                result = new Tuple<PRQAReading, AbstractBuild<?, ?>>();
                result.setSecond(iterate);
                result.setFirst(iterate.getAction(PRQABuildAction.class).getResult());
                return result;
            }
        } while (iterate != null);
        return result;
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

        public FormValidation doCheckVcsConfigXml(@QueryParameter String value) {
            try {
                if (value.endsWith(".xml") || StringUtils.isBlank(value)) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(Messages.PRQANotifier_MustEndWithDotXml());
                }
            } catch (Exception ex) {
                return FormValidation.error(Messages.PRQANotifier_IllegalVcsString());
            }
        }

        public FormValidation doCheckMsgConfigFile(@QueryParameter String value) {
            try {
                if (value.endsWith(".xml") || StringUtils.isBlank(value)) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(Messages.PRQANotifier_MustEndWithDotXml());
                }
            } catch (Exception ex) {
                return FormValidation.error(Messages.PRQANotifier_IllegalVcsString());
            }
        }

        @Override
        public String getDisplayName() {
            return "Programming Research Report";
        }

        public ListBoxModel doFillThreshholdlevelItems() {
            ListBoxModel model = new ListBoxModel();
            for (int i = 0; i < 10; i++) {
                model.add(String.valueOf(i));
            }
            return model;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            final String CHOSEN_REPORT = "chosenReport";
            final String SOURCE_QA_FRAMEWORK = "sourceQAFramework";
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);

            JSONObject sourceObject = formData.getJSONObject(SOURCE_QA_FRAMEWORK);
            if (sourceObject.containsKey(CHOSEN_REPORT)) {
                JSONArray chosenReportArray = sourceObject.getJSONArray(CHOSEN_REPORT);

                QARReportType[] types = getOptionalReportTypes().toArray(
                        new QARReportType[getOptionalReportTypes().size()]);
                instance.setChosenReportTypes(QARReportType.REQUIRED_TYPES.clone());

                for (int i = 0; i < chosenReportArray.size(); i++) {
                    if (chosenReportArray.getBoolean(i) == true) {
                        instance.chosenReportTypes.add(types[i]);
                    }
                }
                instance.chosenReportTypes.add(QARReportType.Compliance);
            }
            if (instance.getGraphTypes() == null || instance.getGraphTypes().isEmpty()) {
                ArrayList<PRQAGraph> list = new ArrayList<PRQAGraph>();
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
                instance.setGraphTypes(list);
            }

            save();
            return instance;
        }

        public EnumSet<QARReportType> getOptionalReportTypes() {
            return QARReportType.OPTIONAL_TYPES;
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

        public void isInstance() {

        }

        public List<QACToolSuite> getQacTools() {
            QACToolSuite[] prqaInstallations = Hudson.getInstance()
                    .getDescriptorByType(QACToolSuite.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }

        public List<QAFrameworkInstallationConfiguration> getQaFrameworkTools() {
            QAFrameworkInstallationConfiguration[] prqaInstallations = Hudson.getInstance()
                    .getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }

        public List<PRQAReportSourceDescriptor<?>> getReportSources() {
            return PostBuildActionSetup.getDescriptors();
        }
    }

    private boolean performQaFrameworkBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

        QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup = (QAFrameworkPostBuildActionSetup) sourceQAFramework;
        QAFrameworkInstallationConfiguration qaFrameworkInstallationConfiguration = QAFrameworkInstallationConfiguration
                .getInstallationByName(qaFrameworkPostBuildActionSetup.qaInstallation);

        outStream.println(VersionInfo.getPluginVersion());

        PRQAToolSuite suite = null;
        if (qaFrameworkInstallationConfiguration != null) {
            suite = qaFrameworkInstallationConfiguration;
        } else {
            try {
                throw new PrqaSetupException(String.format(
                        "The job uses a product configuration (%s) that no longer exists, please reconfigure.", ""));
            } catch (PrqaSetupException pex) {
                outStream.println(pex.getMessage());
                log.log(Level.WARNING, "PrqaException", pex);
                return false;
            }
        }

        outStream.println(Messages.PRQANotifier_ReportGenerateText());
        outStream.println("Workspace : " + build.getWorkspace().getRemote());

        HashMap<String, String> environmentVariables = null;

        if (suite != null) {
            environmentVariables = suite.createEnvironmentVariables(build.getWorkspace().getRemote());
        }

        PRQAApplicationSettings appSettings = new PRQAApplicationSettings(
                qaFrameworkInstallationConfiguration.getHome());
        QaFrameworkReportSettings qaReportSettings;
        try {
            qaReportSettings = setQaFrameworkReportSettings(qaFrameworkPostBuildActionSetup, build);
        } catch (PrqaException ex) {
            log.log(Level.WARNING, "PrqaException", ex.getMessage());
            return false;
        }
        QAVerifyServerSettings qaVerifySettings = setQaVerifyServerSettings(qaFrameworkPostBuildActionSetup.chosenServer);
        QAFrameworkReport report = new QAFrameworkReport(qaReportSettings, qaVerifySettings, appSettings,
                environmentVariables);

        PRQARemoteToolCheck remoteToolCheck = new PRQARemoteToolCheck(new QACli(), environmentVariables, appSettings,
                qaReportSettings, listener, launcher.isUnix());
        QAFrameworkRemoteReport remoteReport = new QAFrameworkRemoteReport(report, listener, launcher.isUnix());
        QAFrameworkRemoteReportUpload remoteReportUpload = new QAFrameworkRemoteReportUpload(report, listener, launcher.isUnix());
        PRQAComplianceStatus currentBuild;
        try {
            currentBuild = performBuild(build, appSettings, remoteToolCheck, remoteReport, qaReportSettings);
        } catch (PrqaException ex) {
            log.log(Level.WARNING, "PrqaException", ex.getMessage());
            return false;
        }

        Tuple<PRQAReading, AbstractBuild<?, ?>> previousBuildResultTuple = getPreviousReading(build, Result.SUCCESS);

        PRQAReading previousStabileBuildResult = previousBuildResultTuple != null ? previousBuildResultTuple.getFirst()
                : null;

        boolean res = true;

        log.fine("thresholdsDesc is null: " + (thresholdsDesc == null));
        if (thresholdsDesc != null) {
            log.fine("thresholdsDescSize: " + thresholdsDesc.size());
        }

        try {
            res = evaluate((PRQAComplianceStatus) previousStabileBuildResult, thresholdsDesc, currentBuild);
            log.fine("Evaluated to: " + res);
        } catch (Exception ex) {
            outStream.println("Report generation ok. Caught exception evaluation results. Trace written to log");
            log.log(Level.SEVERE, "Storing unexpected result evalution exception", ex);
        }

        PRQABuildAction action = new PRQABuildAction(build);
        action.setResult(currentBuild);
        action.setPublisher(this);

        Result buildResult = build.getResult();

        if (!res) {
            if (!buildResult.isWorseOrEqualTo(Result.FAILURE)) {
                build.setResult(Result.UNSTABLE);
            }
            if (qaReportSettings.isPublishToQAV()
                    && !qaReportSettings.isQaUploadWhenStable()
                    && !buildResult.isWorseOrEqualTo(Result.FAILURE)) {
                try {
                    outStream.println("UPLOAD WARNING: Build is Unstable but upload will contunie...");
                    performUpload(build, appSettings, remoteToolCheck, remoteReportUpload);
                } catch (PrqaException ex) {
                    log.log(Level.WARNING, "PrqaException", ex.getMessage());
                    return false;
                }
            } else if (qaReportSettings.isPublishToQAV()
                    && (qaReportSettings.isQaUploadWhenStable()
                    || buildResult.isWorseOrEqualTo(Result.FAILURE))) {
                outStream.println("UPLOAD WARNING: QAV Upload cant be perform because build is Unstable");
                log.warning("UPLOAD WARNING - QAV Upload cant be perform because build is Unstable");
            }

        } else if (qaReportSettings.isPublishToQAV()) {
            if (buildResult.isWorseOrEqualTo(Result.FAILURE)
                    && qaReportSettings.isQaUploadWhenStable()) {
                outStream.println("UPLOAD WARNING: QAV Upload cant be perform because build is Unstable");
                log.warning("UPLOAD WARNING - QAV Upload cant be perform because build is Unstable");
            } else {
                try {
                    outStream.println("UPLOAD INFO: QAV Upload...");
                    performUpload(build, appSettings, remoteToolCheck, remoteReportUpload);
                } catch (PrqaException ex) {
                    log.log(Level.WARNING, "PrqaException", ex.getMessage());
                    return false;
                }
            }
        }
        outStream.println("\n----------------------BUILD Results-----------------------\n");
        if (previousBuildResultTuple != null) {
            outStream.println(String.format(Messages.PRQANotifier_PreviousResultBuildNumber(new Integer(previousBuildResultTuple.getSecond().number))));
            outStream.println(previousBuildResultTuple.getFirst());

        } else {
            outStream.println(Messages.PRQANotifier_NoPreviousResults());
        }
        outStream.println(Messages.PRQANotifier_ScannedValues());
        outStream.println(currentBuild);

        build.getActions().add(action);
        return true;
    }

    private QaFrameworkReportSettings setQaFrameworkReportSettings(
            QAFrameworkPostBuildActionSetup qaFrameworkPostBuildActionSetup, AbstractBuild<?, ?> build)
            throws PrqaSetupException {

        if (qaFrameworkPostBuildActionSetup.qaProject != null) {

            return new QaFrameworkReportSettings(qaFrameworkPostBuildActionSetup.qaInstallation,
                    qaFrameworkPostBuildActionSetup.qaProject, qaFrameworkPostBuildActionSetup.downloadUnifiedProjectDefinition,
                    qaFrameworkPostBuildActionSetup.unifiedProjectName, qaFrameworkPostBuildActionSetup.enableMtr,
                    qaFrameworkPostBuildActionSetup.enableProjectCma, qaFrameworkPostBuildActionSetup.enableDependencyMode,
                    qaFrameworkPostBuildActionSetup.performCrossModuleAnalysis, qaFrameworkPostBuildActionSetup.CMAProjectName,
                    qaFrameworkPostBuildActionSetup.generateReport, qaFrameworkPostBuildActionSetup.publishToQAV,
                    qaFrameworkPostBuildActionSetup.loginToQAV, product, qaFrameworkPostBuildActionSetup.uploadWhenStable,
                    qaFrameworkPostBuildActionSetup.qaVerifyProjectName, qaFrameworkPostBuildActionSetup.uploadSnapshotName,
                    Integer.toString(build.getNumber()), qaFrameworkPostBuildActionSetup.uploadSourceCode, qaFrameworkPostBuildActionSetup.generateCrr,
                    qaFrameworkPostBuildActionSetup.generateMdr, qaFrameworkPostBuildActionSetup.generateSup, qaFrameworkPostBuildActionSetup.analysisSettings,
                    qaFrameworkPostBuildActionSetup.stopWhenFail, qaFrameworkPostBuildActionSetup.generatePreprocess, qaFrameworkPostBuildActionSetup.assembleSupportAnalytics);
        }
        throw new PrqaSetupException("Please set a project in Qa Framework section configuration!");
    }

// Function to pull details from QAV Configuration.
    private QAVerifyServerSettings setQaVerifyServerSettings(String configurationByName) {

        QAVerifyServerConfiguration qaVerifyServerConfiguration = PRQAGlobalConfig.get().getConfigurationByName(
                configurationByName);
        if (qaVerifyServerConfiguration != null) {
            return new QAVerifyServerSettings(qaVerifyServerConfiguration.getHostName(),
                    qaVerifyServerConfiguration.getViewerPortNumber(), qaVerifyServerConfiguration.getProtocol(),
                    qaVerifyServerConfiguration.getPassword(), qaVerifyServerConfiguration.getUserName());
        }
        return new QAVerifyServerSettings();

    }

    private PRQAComplianceStatus performBuild(AbstractBuild<?, ?> build, PRQAApplicationSettings appSettings,
            PRQARemoteToolCheck remoteToolCheck, QAFrameworkRemoteReport remoteReport,
            QaFrameworkReportSettings qaReportSettings) throws PrqaException {

        boolean success = true;
        PRQAComplianceStatus currentBuild = null;

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(build.getWorkspace().act(remoteToolCheck));
            success = isQafVersionSupported(qaFrameworkVersion);
            if (!success) {
                build.setResult(Result.FAILURE);
                throw new PrqaException("Build failure. Plese upgrade to a newer version of Qa Framework");
            }
            remoteReport.setQaFrameworkVersion(qaFrameworkVersion);
            currentBuild = build.getWorkspace().act(remoteReport);
            currentBuild.setMessagesWithinThresholdForEachMessageGroup(threshholdlevel);
        } catch (IOException ex) {
            success = false;
            outStream.println(ex.getMessage());
            build.setResult(Result.FAILURE);
            throw new PrqaException("IO exception. Please retry.");
        } catch (Exception ex) {
            outStream.println(Messages.PRQANotifier_FailedGettingResults());
            outStream.println(ex.getMessage());
            log.log(Level.SEVERE, "Unhandled exception", ex);
            success = false;
            build.setResult(Result.FAILURE);
            throw new PrqaException("IO exception. Please retry.");
        } finally {
            if (success) {
                copyArtifacts(build, qaReportSettings);
            }
        }
        return currentBuild;
    }

    private PRQAComplianceStatus performUpload(AbstractBuild<?, ?> build, PRQAApplicationSettings appSettings,
            PRQARemoteToolCheck remoteToolCheck, QAFrameworkRemoteReportUpload remoteReportUpload) throws PrqaException {

        boolean success = true;
        PRQAComplianceStatus currentBuild = null;

        try {
            QaFrameworkVersion qaFrameworkVersion = new QaFrameworkVersion(build.getWorkspace().act(remoteToolCheck));
            success = isQafVersionSupported(qaFrameworkVersion);
            if (!success) {
                build.setResult(Result.FAILURE);
                throw new PrqaException("Build failure. Plese upgrade to a newer version of Qa Framework");
            }
            remoteReportUpload.setQaFrameworkVersion(qaFrameworkVersion);
            currentBuild = build.getWorkspace().act(remoteReportUpload);
        } catch (IOException ex) {
            success = false;
            outStream.println(ex.getMessage());
            build.setResult(Result.FAILURE);
            throw new PrqaException("IO exception. Please retry.");
        } catch (Exception ex) {
            outStream.println(Messages.PRQANotifier_FailedGettingResults());
            outStream.println(ex.getMessage());
            log.log(Level.SEVERE, "Unhandled exception", ex);
            success = false;
            build.setResult(Result.FAILURE);
            throw new PrqaException("IO exception. Please retry.");
        }
        return currentBuild;
    }

    private boolean isQafVersionSupported(QaFrameworkVersion qaFrameworkVersion) {
        String shortVersion = qaFrameworkVersion.getVersionShortFormat();
        String qafVersion = shortVersion.substring(shortVersion.lastIndexOf(" ") + 1);
        if (qaFrameworkVersion == null) {
            return false;
        }
        outStream.println("PRQA Source Code Analysis Framework " + qafVersion);
        if (!qaFrameworkVersion.isQAFVersionSupported()) {
            outStream.println(String.format(
                    "Your QA·CLI version is %s.In order to use our product install a newer version of PRQA·Framework!",
                    qaFrameworkVersion.getQaFrameworkVersion()));
            return false;
        }
        return true;
    }

    private void copyArtifacts(AbstractBuild<?, ?> build, QaFrameworkReportSettings qaReportSettings) {

        try {
            copyReportsToArtifactsDir(qaReportSettings, build);
            if (qaReportSettings.isPublishToQAV() && qaReportSettings.isLoginToQAV()) {
                copyReourcesToArtifactsDir("*.log", build);
            }
        } catch (Exception ex) {
            outStream.println("Error in copying artifacts to artifact dir");
            log.log(Level.SEVERE, "Failed copying build artifacts", ex);
        }
    }

    public void setQaFrameworkVersion(QaFrameworkVersion qaFrameworkVersion) {
        this.qaFrameworkVersion = qaFrameworkVersion;
    }
}
