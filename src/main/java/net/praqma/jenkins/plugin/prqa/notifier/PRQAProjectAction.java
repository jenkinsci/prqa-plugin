package net.praqma.jenkins.plugin.prqa.notifier;

import com.google.common.collect.Iterables;
import hudson.model.Actionable;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.Publisher;
import hudson.util.RunList;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.graphs.JSONGraph;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.StatusCategory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Praqma
 */
public class PRQAProjectAction
        extends Actionable
        implements ProminentProjectAction, PrqaProjectName, IndexedAction {

    private final Job<?, ?> job;
    private static final String ICON_NAME = "/plugin/prqa-plugin/images/32x32/helix_qac.png";
    private String fullPrqaProjectName;
    private String displayName;
    private int index;

    public PRQAProjectAction(Job<?, ?> job, final String fullPrqaProjectName) {
        this.job = job;
        this.index = getProjectActionIndex();
        this.fullPrqaProjectName = fullPrqaProjectName;
        setDisplayName();
    }

    private void setDisplayName() {
        String pattern = Pattern.quote(File.separator);
        String[] parts = this.fullPrqaProjectName.split(pattern);
        this.displayName = parts.length >= 1 ? parts[parts.length - 1] : "";
    }

    @Override
    public int getIndex() {
        return getProjectActionIndex();
    }

    @Override
    public String getDisplayName() {
        if (isPrimaryStep()) return "Helix QAC Results";
        return null;
    }

    public String getDisplayName2() {
        return this.displayName;
    }

    public String getProjectName() {
        return this.fullPrqaProjectName;
    }

    @Override
    public String getSearchUrl() {
        if (isPrimaryStep()) return "PRQA";
        return null;
    }

    @Override
    public String getIconFileName() {
        if(isPrimaryStep()) return ICON_NAME;
        return null;
    }

    @Override
    public String getUrlName() {
        if(isPrimaryStep()) return "PRQA";
        return null;
    }

    public PRQABuildAction getLatestActionInProject() {
        if (job == null) return null;
        Run<?,?> lastSuccessfulBuild = job.getLastSuccessfulBuild();
        this.index = getProjectActionIndex();
        if (lastSuccessfulBuild != null) {
            List<PRQABuildAction> buildActions = lastSuccessfulBuild.getActions(PRQABuildAction.class);
            if (buildActions.size() > 0 && index < buildActions.size() && index > -1) return buildActions.get(index);
        }
        return null;
    }

    private int getProjectActionIndex() {
        if (job == null || job instanceof FreeStyleProject) {
            return 0;
        }
        int index=0;
        List<PRQAProjectAction> projectActions = job.getActions(PRQAProjectAction.class);
        for(PRQAProjectAction projectAction : projectActions) {
            if (projectAction == this) break;
            index++;
        }
        return index;
    }

    public List<PRQAProjectAction> getAllProjectActions() {
        return job != null ? job.getActions(PRQAProjectAction.class) : null;
    }

    public List<PRQABuildAction> getLatestActions() {
        Run<?,?> lastSuccessfulBuild = job.getLastSuccessfulBuild();
        if (lastSuccessfulBuild != null) {
            return lastSuccessfulBuild.getActions(PRQABuildAction.class);
        }
        return null;
    }

    /**
     * Small method to determine whether to draw graphs or not.
     *
     * @return true when there are more than 2 or more builds available.
     */
    public boolean isDrawGraphs() {
        RunList<?> builds = job.getBuilds();
        return Iterables.size(builds) >= 2;
    }

    public JSONGraph getChartData(final PRQABuildAction buildAction, final int width, final int height, final String className, final int thresholdLevel) {
        // determine type of graph to display
        boolean isComplianceIndexGraph = className.equalsIgnoreCase("ComplianceIndexGraphs");

        // instantiate DTO for capturing graph data
        JSONGraph graph = new JSONGraph(buildAction.getProjectName(), isComplianceIndexGraph, thresholdLevel);
        List<String> xAxisLabels = new ArrayList<>();
        List<Double> primary_values = new ArrayList<>();
        List<Double> threshold_values = new ArrayList<>();
        List<Double> secondary_values = new ArrayList<>();

        // iterate through historical builds
        Run<?,?> build = job != null ? job.getFirstBuild() : null;
        if (build != null) {
            do {
                PRQABuildAction historicBuildAction = getBuildActionByIndex(build, index);
                if (historicBuildAction != null) {
                    Result result = build.getResult();
                    if (result != null && (result == Result.SUCCESS || result == Result.UNSTABLE)) {
                        xAxisLabels.add("#".concat(build.getId()));
                        PRQAReading stat = historicBuildAction.getResult();
                        try {
                            PRQAComplianceStatus cs = (PRQAComplianceStatus) stat;
                            if (!isComplianceIndexGraph) {
                                Integer value = cs.getMessagesWithinThresholdCount(thresholdLevel);
                                primary_values.add(value.doubleValue());
                            } else {
                                primary_values.add(stat.getReadout(StatusCategory.ProjectCompliance).doubleValue());
                                secondary_values.add(stat.getReadout(StatusCategory.FileCompliance).doubleValue());
                            }
                        } catch (PrqaException ex) {
                            continue;
                        }
                    }
                }
                build = build.getNextBuild();
            } while(build != null);
        }
        graph.setSecondaryValues(secondary_values);
        graph.setPrimaryValues(primary_values);
        graph.setThresholdValues(threshold_values);
        graph.setxAxisLabels(xAxisLabels);
        return graph;
    }

    private PRQABuildAction getBuildActionByIndex(Run<?,?> build, int index) {
        List<PRQABuildAction> buildActions = build.getActions(PRQABuildAction.class);
        if (index > -1 && index < buildActions.size()) return buildActions.get(index);
        return null;
    }

    public QAVerifyServerConfiguration getConfiguration() {
        PRQABuildAction action = this.getLatestActionInProject();
        if(action != null) {
            Publisher publisher = action.getPublisher();
            if(publisher != null && publisher instanceof  PRQANotifier) {
                PRQANotifier notifier = (PRQANotifier)publisher;
                QAVerifyServerConfiguration qavconfig;
                List<String> servers = notifier.sourceQAFramework.chosenServers;
                String chosenServer = servers != null && !servers.isEmpty() ? servers.get(0) : null;
                qavconfig = PRQAGlobalConfig.get()
                        .getConfigurationByName(chosenServer);
                return qavconfig;
            }
        }
        return null;
    }

    public Collection<QAVerifyServerConfiguration> getConfigurations() {
        PRQABuildAction action = this.getLatestActionInProject();
        if(action != null) {
            Publisher publisher = action.getPublisher();
            if(publisher != null && publisher instanceof  PRQANotifier) {
                PRQANotifier notifier = (PRQANotifier)publisher;
                if (notifier != null) {
                    Collection<QAVerifyServerConfiguration> qavconfig;
                    qavconfig = PRQAGlobalConfig.get()
                            .getConfigurationsByNames(notifier.sourceQAFramework.chosenServers);
                    return qavconfig;
                }
            }
        }
        return null;
    }

    public boolean showLinksofInterest() {
        PRQABuildAction action = this.getLatestActionInProject();
        if(action != null) {
            Publisher publisher = action.getPublisher();
            if(publisher != null && publisher instanceof  PRQANotifier) {
                PRQANotifier notifier = (PRQANotifier)publisher;
                if (notifier != null) {
                    if (notifier.sourceQAFramework != null) {
                        return notifier.sourceQAFramework.loginToQAV;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPrimaryStep() {
        if (job == null) return true;
        Run<?,?> lastBuild = job.getLastBuild();
        if (lastBuild != null) {
            PRQABuildAction buildAction = lastBuild.getAction(PRQABuildAction.class);
            if (buildAction != null) {
                return buildAction.getProjectName().equals(this.fullPrqaProjectName);
            }
        }
        return false;
    }
}
