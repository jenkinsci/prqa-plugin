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
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.tasks.Publisher;
import java.util.Iterator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Praqma
 */
public class PRQABuildAction implements Action {
    
    private final AbstractBuild<?,?> build;
    private Publisher publisher; 
    
    public PRQABuildAction(AbstractBuild<?,?> build) {
        this.build = build;
    }

    @Override
    public String getIconFileName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDisplayName() {
        return "PRQA";
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
     * @param publisher the publisher to set
     */
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
    
    /**
     * Used to cycle through all previous builds.
     * @return 
     */
    public PRQABuildAction getPreviousAction() {      
        return getPreviousAction(build);
    }
    
    public PRQABuildAction getPreviousAction(AbstractBuild<?,?> base) {
        PRQABuildAction action = null;
        AbstractBuild<?,?> start = base;
        while(true) {
            start = start.getPreviousNotFailedBuild();
            if(start == null)
                return null;
            action = start.getAction(PRQABuildAction.class);
            return action;
        }
    }
    
    public Status getBuildActionStatus() {
        return ((PRQANotifier)publisher).getStatus();
    }
    
    /**
     * Do statistics based on the actions of the build.
     * @param req
     * @param rsp 
     */
    public void doComplianceStatistics(StaplerRequest req, StaplerResponse rsp) {
        for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
            
        }
    }

}
