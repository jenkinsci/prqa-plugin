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
    
    public PRQAProjectAction(AbstractProject<?,?> project) {
        this.project = project;
    }
    
    @Override
    public String getDisplayName() {
        return "PRQA Project";
    }

    @Override
    public String getSearchUrl() {
        return getUrlName();
    }

    @Override
    public String getIconFileName() {
        throw new UnsupportedOperationException("There is no icon for this project");
    }

    @Override
    public String getUrlName() {
        return "PRQA";
    }
    
    public PRQABuildAction getLatestActionInProject() {
        return project.getLastSuccessfulBuild().getAction(PRQABuildAction.class);     
    }
    
    public void doComplianceStatistics(StaplerRequest req, StaplerResponse rsp) {
        PRQABuildAction action = getLatestActionInProject();       
        if(action != null) { 
            try {
                action.doComplianceStatistics(req, rsp);
            } catch (IOException exception) {
                
            }
        }
    } 
}
