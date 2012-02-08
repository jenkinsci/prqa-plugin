/*
 * The MIT License
 *
 * Copyright 2012 jes.
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
/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;


import hudson.Extension;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

//TODO maybe it should extends Recorder
public class PRQANotifier extends Notifier {
    //Default null construtor

    public PRQANotifier() {
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        //TODO copied from CCUCM NOTIFIER ask chw why this is like this????
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        return false;
    }

    private void execute(String cmd) {
    }
}

/**
 * This class is used by Jenkins to define the plugin.
 * 
 * @author jes
 */
@Extension
public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
        super(PRQANotifier.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "PRQA Report";
    }

    @Override
    public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        save();
        return new PRQANotifier();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> arg0) {
        return true;
    }
}
