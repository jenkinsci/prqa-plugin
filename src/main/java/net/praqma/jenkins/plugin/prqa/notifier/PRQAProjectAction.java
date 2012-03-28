package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Praqma
 */
public class PRQAProjectAction extends Actionable implements ProminentProjectAction {

    private final AbstractProject<?,?> project;
    public static final String PROJECT_WIKI = "https://wiki.jenkins-ci.org/display/JENKINS/PRQA+Plugin";
    
    public PRQAProjectAction(AbstractProject<?,?> project) {
        this.project = project;
    }
    
    @Override
    public String getDisplayName() {
        return "PRQA Project";
    }

    @Override
    public String getSearchUrl() {
        return "PRQA";
    }

    @Override
    public String getIconFileName() {
        return PRQABuildAction.ICON_NAME;
    }

    @Override
    public String getUrlName() {
        return "PRQA";
    }
    
    public PRQABuildAction getLatestActionInProject() {
        if(project.getLastSuccessfulBuild() != null) {
            return project.getLastSuccessfulBuild().getAction(PRQABuildAction.class);     
        }
        return null;
    }
    
    /**
     * New one.  
     * @param req
     * @param rsp 
     */
    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) {
        PRQABuildAction action = getLatestActionInProject();
        if(action != null) { 
            try {
                action.doReportGraphs(req, rsp);
            } catch (IOException exception) {
                
            }
        }
    }
}
