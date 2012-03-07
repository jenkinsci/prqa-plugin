/*
 * The MIT License
 *
 * Copyright 2012 Praqma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
        if(action != null)
            try {
                action.doComplianceStatistics(req, rsp);
            } catch (IOException exception) {
                
            }           
    }
 
}
