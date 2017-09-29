package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public abstract class PRQAFileProjectSourceDescriptor<T extends PRQAFileProjectSource>
        extends Descriptor<PRQAFileProjectSource> {
    public PRQAFileProjectSource newInstance(StaplerRequest req,
                                             JSONObject formData,
                                             PRQAFileProjectSource instance)
            throws
            FormException {
        return super.newInstance(req,
                                 formData);
    }
}
