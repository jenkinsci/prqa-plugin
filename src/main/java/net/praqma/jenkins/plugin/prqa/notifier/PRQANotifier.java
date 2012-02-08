/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import java.io.*;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

//TODO maybe it should extends Recorder
public class PRQANotifier extends Publisher {

    private PrintStream out;
    private Boolean totalBetter;
    private int totalMax;

    @DataBoundConstructor
    public PRQANotifier(boolean totalBetter, String totalMax) {
        this.totalBetter = totalBetter;
        this.totalMax = Integer.parseInt(totalMax);

    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        //TODO copied from CCUCM NOTIFIER ask chw why this is like this????
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        out = listener.getLogger();

        build.getDescription();

        out.println(build.getDisplayName());
        out.println("[" + totalBetter + "]");
        out.println("[" + totalMax + "]");

        return true;
    }

    @Exported
    public boolean getTotalBetter() {
        return totalBetter;
    }

    @Exported
    public int getTotalMax() {
        return totalMax;
    }

    /**
     * This class is used by Jenkins to define the plugin.
     * 
     * @author jes
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String prqaHome;

        @Override
        public String getDisplayName() {
            return "Programming Research Report";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public PRQANotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);
            System.out.println(instance.totalBetter);
            System.out.print(instance.totalMax);
            return instance;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this);
            save();
            return super.configure(req, formData);
        }

        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }
     
    }
}

