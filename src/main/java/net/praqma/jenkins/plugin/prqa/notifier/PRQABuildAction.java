package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.qaframework.QaFrameworkReportSettings;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.status.StatusCategory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Praqma
 */
public class PRQABuildAction
        implements Action, SimpleBuildStep.LastBuildAction, PrqaProjectName, IndexedAction {

    private final Run<?, ?> build;
    private Publisher publisher;
    private PRQAReading result;
    private List<PRQAProjectAction> projectActions;
    private String fullPrqaProjectName;
    private boolean isPrimary;
    private int index;

    public PRQABuildAction() {
        this.build = null;
        getProjectName();
        this.isPrimary = isPrimaryStep();
    }

    public PRQABuildAction(Run<?, ?> build, final String fullPrqaProjectName, int index) {
        this.build = build;
        this.index = index;
        this.isPrimary = true;
        this.fullPrqaProjectName = fullPrqaProjectName == null ? getProjectName() : fullPrqaProjectName;
        this.projectActions = build.getParent().getActions(PRQAProjectAction.class);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        if (this.isPrimary) return "PRQA";
        return null;
    }

    public String getDisplayName2() {
        String pattern = Pattern.quote(File.separator);
        String[] parts = this.getProjectName().split(pattern);
        return parts.length >= 1 ? parts[parts.length - 1] : "";
    }

    public String getProjectName() {
        if (this.fullPrqaProjectName == null) {
            PRQANotifier notifier = this.getPublisher(PRQANotifier.class);
            if (notifier != null) {
                this.fullPrqaProjectName = notifier.sourceQAFramework.qaProject;
            }
        }
        return this.fullPrqaProjectName;
    }

    @Override
    public String getUrlName() {
        if (isPrimaryStep()) return "PRQA";
        return null;
    }

    /**
     * @return the publisher
     */
    public Publisher getPublisher() {
        return publisher;
    }

    /**
     * @param clazz class
     * @param <T> type
     * @return publisher for class
     */
    @SuppressWarnings("unchecked")
    public <T extends Publisher> T getPublisher(Class<T> clazz) {
        try {
            return clazz.cast(publisher);
        } catch (Exception e) {
            return (T) publisher;
        }
    }

    public PRQAReading getResult() {
        return this.result;
    }

    public Number getThreshold(StatusCategory cat) {
        if (this.result != null && this.result.getThresholds()
                .containsKey(cat)) {
            return result.getThresholds()
                    .get(cat);
        }
        return null;
    }

    /**
     * Converts the result of the interface to a concrete implementation. Returns null in cases where it is not possible.
     * <p>
     * This check is needed if you for some reason decide to switch report type on the same job, since each report has it's own implementation
     * of a status. We need to do a check on all the collected results to get those that fits the current job profile.
     *
     * @param <T> type
     * @param clazz class
     * @return result
     */
    @SuppressWarnings("unchecked")
    public <T extends PRQAStatus> T getResult(Class<T> clazz) {
        try {
            return clazz.cast(this.result);
        } catch (Exception nex) {
            return (T) this.result;
        }
    }

    public void setResult(PRQAReading result) {
        this.result = result;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Used to cycle through all previous builds.
     *
     * @return previous build
     */
    public PRQABuildAction getPreviousAction(int index) {
        return getPreviousAction(build, index);
    }

    /**
     * Fetches the previous PRQA build. Skips builds that were not configured as a PRQA Build.
     * <p>
     * Goes to the end of list.
     * @param run: the base
     * @return previous build
     */
    public PRQABuildAction getPreviousAction(Run<?, ?> run, int index) {
        Run<?,?> lastRun = run.getPreviousNotFailedBuild();
        if (lastRun == null) {
            return null;
        }

        List<PRQABuildAction> buildActions = lastRun.getActions(PRQABuildAction.class);
        if (buildActions == null) return getPreviousAction(lastRun, index);

        if (index > -1 && index < buildActions.size()) return buildActions.get(index);
        return null;
    }

    public PRQAReading getBuildActionStatus() {
        return this.result;
    }

    @SuppressWarnings("unchecked")
    public <T extends PRQAStatus> T getBuildActionStatus(Class<T> clazz) {
        if (this.result != null) {
            if (this.result instanceof PRQAComplianceStatus) {
                PRQAComplianceStatus status = (PRQAComplianceStatus) this.result;
                if (status.getSettings() == null ) {
                    QaFrameworkReportSettings settings = getFrameworkReportSettings();
                    status.setSettings(settings);
                }
            }
        }

        try {
            return clazz.cast(this.result);
        } catch (Exception e) {
            return (T) this.result;
        }
    }

    private QaFrameworkReportSettings getFrameworkReportSettings() {
        PRQANotifier notifier = this.getPublisher(PRQANotifier.class);
        if (notifier != null) {
            if (notifier.sourceQAFramework != null) {
                return new QaFrameworkReportSettings(
                        notifier.sourceQAFramework.qaInstallation,
                        notifier.sourceQAFramework.useCustomLicenseServer,
                        notifier.sourceQAFramework.customLicenseServerAddress,
                        notifier.sourceQAFramework.qaProject,
                        notifier.sourceQAFramework.downloadUnifiedProjectDefinition,
                        notifier.sourceQAFramework.unifiedProjectName,
                        notifier.sourceQAFramework.enableDependencyMode,
                        notifier.sourceQAFramework.performCrossModuleAnalysis,
                        notifier.sourceQAFramework.cmaProjectName,
                        notifier.sourceQAFramework.reuseCmaDb,
                        notifier.sourceQAFramework.useDiskStorage,
                        notifier.sourceQAFramework.generateReport,
                        notifier.sourceQAFramework.publishToQAV,
                        notifier.sourceQAFramework.loginToQAV,
                        notifier.sourceQAFramework.uploadWhenStable,
                        notifier.sourceQAFramework.qaVerifyProjectName,
                        notifier.sourceQAFramework.uploadSnapshotName,
                        notifier.sourceQAFramework.buildNumber,
                        notifier.sourceQAFramework.uploadSourceCode,
                        notifier.sourceQAFramework.generateCrr,
                        notifier.sourceQAFramework.generateMdr,
                        notifier.sourceQAFramework.generateHis,
                        notifier.sourceQAFramework.generateSup,
                        notifier.sourceQAFramework.analysisSettings,
                        notifier.sourceQAFramework.stopWhenFail,
                        notifier.sourceQAFramework.customCpuThreads,
                        notifier.sourceQAFramework.maxNumThreads,
                        notifier.sourceQAFramework.generatePreprocess,
                        notifier.sourceQAFramework.assembleSupportAnalytics,
                        notifier.sourceQAFramework.generateReportOnAnalysisError,
                        notifier.sourceQAFramework.addBuildNumber,
                        notifier.sourceQAFramework.projectConfiguration
                );
            }
        }
        return null;
    }

    /**
     * Determines weather to draw threhshold graphs. Uses the most recent build as base.
     *
     * @param req
     * @param rsp
     * @return
     */
    private HashMap<StatusCategory, Boolean> _doDrawThresholds(StaplerRequest req,
                                                               StaplerResponse rsp, PRQABuildAction buildAction) {

        PRQANotifier notifier = buildAction.getPublisher(PRQANotifier.class);

        HashMap<StatusCategory, Boolean> stats = new HashMap<>();
        if (notifier != null) {
            String className = req.getParameter("graph");
            PRQAGraph graph = notifier.getGraph(className);
            for(PRQABuildAction prqabuild = buildAction; prqabuild != null; prqabuild = prqabuild.getPreviousAction(prqabuild.getIndex())) {
                if (prqabuild.getResult() != null) {
                    for (StatusCategory cat : graph.getCategories()) {
                        Number threshold = prqabuild.getThreshold(cat);
                        if (threshold != null) {
                            stats.put(cat, Boolean.TRUE);
                        } else {
                            stats.put(cat, Boolean.FALSE);
                        }
                    }
                    return stats;
                }
            }
        }
        return stats;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (this.fullPrqaProjectName == null) {
            getProjectName();
        }
        return this.projectActions;
    }

    public List<PRQAGraph> getSupportedGraphs() {
        PRQANotifier notifier = this.getPublisher(PRQANotifier.class);
        if (notifier != null) {
            return notifier.getSupportedGraphs();
        }
        return null;
    }

    public int getThresholdLevel() {
        PRQANotifier notifier = this.getPublisher(PRQANotifier.class);
        if (notifier != null) {
            return notifier.getThresholdLevel();
        }
        return 0;
    }

    public Run<?,?> getBuild() {
        return this.build;
    }

    private boolean isPrimaryStep() {
        Run<?,?> lastBuild = build.getPreviousBuild();
        if (lastBuild != null) {
            PRQABuildAction buildAction = lastBuild.getAction(PRQABuildAction.class);
            return buildAction.getIndex() == 0;
        }
        return false;
    }
}
