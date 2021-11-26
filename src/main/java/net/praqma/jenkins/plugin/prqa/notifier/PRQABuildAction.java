package net.praqma.jenkins.plugin.prqa.notifier;


import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.Publisher;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import jenkins.tasks.SimpleBuildStep;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.qaframework.QaFrameworkReportSettings;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.status.StatusCategory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Praqma
 */
public class PRQABuildAction
        implements Action, SimpleBuildStep.LastBuildAction, PrqaProjectName {

    private final Run<?, ?> build;
    private Publisher publisher;
    private PRQAReading result;
    private List<PRQAProjectAction> projectActions;
    private String fullPrqaProjectName;

    public PRQABuildAction() {
        this.build = null;
        getProjectName();
    }

    public PRQABuildAction(Run<?, ?> build, final String fullPrqaProjectName) {
        this.build = build;
        this.fullPrqaProjectName = fullPrqaProjectName == null ? getProjectName() : fullPrqaProjectName;
        List<PRQAProjectAction> projectActions = new ArrayList<>();
        projectActions.add( new PRQAProjectAction( build.getParent(), fullPrqaProjectName ) );
        this.projectActions = projectActions;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
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
        return "PRQA";
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
    public PRQABuildAction getPreviousAction(String fullPrqaProjectName) {
        return getPreviousAction(build, fullPrqaProjectName);
    }

    /**
     * Fetches the previous PRQA build. Skips builds that were not configured as a PRQA Build.
     * <p>
     * Goes to the end of list.
     * @param run: the base
     * @return previous build
     */
    public PRQABuildAction getPreviousAction(Run<?, ?> run, String projectName) {
        Run<?,?> lastRun = run.getPreviousNotFailedBuild();
        if (lastRun == null) {
            return null;
        }

        List<PRQABuildAction> buildActions = lastRun.getActions(PRQABuildAction.class);
        if (buildActions == null) return getPreviousAction(projectName);

        for(PRQABuildAction buildAction : buildActions) {
            PRQANotifier notifier = buildAction.getPublisher(PRQANotifier.class);
            if (notifier != null) {
                if (notifier.sourceQAFramework.qaProject == projectName) {
                    return buildAction;
                }
            }
        }
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

    public StatusCategory[] getComplianceCategories() {
        return StatusCategory.values();
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

    private PRQABuildAction getBuildActionByProject(String project) {
        List<PRQABuildAction> buildActions = this.build.getActions(PRQABuildAction.class);
        for (PRQABuildAction buildAction : buildActions) {
            if (buildAction.getProjectName().equalsIgnoreCase(project)) return  buildAction;
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
            for(PRQABuildAction prqabuild = buildAction; prqabuild != null; prqabuild = prqabuild.getPreviousAction(prqabuild.getProjectName())) {
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

    /**
     * This function works in the following way:
     * <p>
     * After choosing your report type, as set of supported graphs are given, which it is up to the user to add. Currently this is done programatically, but given my design, it should be relatively simple to make
     * this possible to edit in the GUI.
     * <p>
     * If a result is fetched and it does not contain the property to draw the graphs the report demands we simply skip it. This means you can switch report type in a job. You don't need
     * to create a new job if you just want to change reporting mode.
     * <p>
     * This method catches the PrqaReadingException, when that exception is thrown it means that the we skip the reading and continue.
     *
     * @param req request
     * @param rsp response
     * @throws IOException when exception
     */
    public void doReportGraphs(StaplerRequest req,
                               StaplerResponse rsp)
            throws IOException {

        List<PRQABuildAction> buildActions = this.build.getActions(PRQABuildAction.class);
        Integer tSetting = Integer.parseInt(req.getParameter("tsetting"));
        String className = req.getParameter("graph");
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<>();
        Double tMax = null;
        PRQAGraph graph = null;
        PRQAStatusCollection collection = new PRQAStatusCollection();

        for(PRQABuildAction buildAction : buildActions) {
            PRQANotifier notifier = buildAction.getPublisher(PRQANotifier.class);
            graph = notifier.getGraph(className);
            ChartUtil.NumberOnlyBuildLabel label;

            for (PRQABuildAction prqabuild = buildAction; prqabuild != null; prqabuild = prqabuild.getPreviousAction(prqabuild.getProjectName())) {
                if (prqabuild.getResult() != null) {
                    label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build);
                    PRQAReading stat = prqabuild.getResult();
                    for (StatusCategory cat : graph.getCategories()) {
                        Number res;
                        try {
                            PRQAComplianceStatus cs = (PRQAComplianceStatus) stat;
                            if (cat.equals(StatusCategory.Messages)) {
                                res = cs.getMessagesWithinThresholdCount(tSetting);
                            } else {
                                res = stat.getReadout(cat);
                            }
                        } catch (PrqaException ex) {
                            continue;
                        }

                        HashMap<StatusCategory, Boolean>  drawMatrix = _doDrawThresholds(req, rsp, prqabuild);
                        if (drawMatrix.containsKey(cat) && drawMatrix.get(cat)) {
                            Number threshold = buildAction.getThreshold(cat);
                            if (threshold != null) {
                                if (tMax == null) {
                                    tMax = threshold.doubleValue();
                                } else if (tMax < threshold.doubleValue()) {
                                    tMax = threshold.doubleValue();
                                }

                                dsb.add(threshold, String.format("%s Threshold", cat.toString()), label);
                            }
                        }
                        dsb.add(res, cat.toString(), label);
                        collection.add(stat);
                    }
                }
            }
        }
        if (graph != null) {
            graph.setData(collection);
            graph.drawGraph(req, rsp, dsb, tMax);
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (this.fullPrqaProjectName == null) {
            getProjectName();
        }
        if (this.projectActions.get(0).getProjectName() == null) {
            this.projectActions.clear();
            this.projectActions.add(new PRQAProjectAction(build.getParent(), this.fullPrqaProjectName));
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
}
