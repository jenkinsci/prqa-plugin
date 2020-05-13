package net.praqma.jenkins.plugin.prqa.notifier;

import com.google.common.collect.Iterables;
import hudson.model.Actionable;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.tasks.Publisher;
import hudson.util.RunList;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Praqma
 */
public class PRQAProjectAction
        extends Actionable
        implements ProminentProjectAction {

    private final Job<?, ?> job;
    private static final String ICON_NAME = "/plugin/prqa-plugin/images/32x32/helix_qac.png";


    public PRQAProjectAction(Job<?, ?> job) {
        this.job = job;
    }


    @Override
    public String getDisplayName() {
        return "Helix QAC Results";
    }


    @Override
    public String getSearchUrl() {
        return "PRQA";
    }


    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }


    @Override
    public String getUrlName() {
        return "PRQA";
    }


    public Job<?, ?> getJob() {
        return job;
    }


    public PRQABuildAction getLatestActionInProject() {
        if (job.getLastSuccessfulBuild() != null) {
            return job.getLastSuccessfulBuild()
                    .getAction(PRQABuildAction.class);
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


    /**
     * New one.
     *
     * @param req request
     * @param rsp response
     */
    public void doReportGraphs(StaplerRequest req,
                               StaplerResponse rsp) {
        PRQABuildAction action = getLatestActionInProject();
        if (action != null) {
            try {
                action.doReportGraphs(req, rsp);
            } catch (IOException exception) {

            }
        }
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
}
