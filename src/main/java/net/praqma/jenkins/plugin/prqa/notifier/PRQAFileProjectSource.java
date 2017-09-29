package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PRQAFileProjectSource
        implements Describable<PRQAFileProjectSource>,
                   ExtensionPoint,
                   Serializable {

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<PRQAFileProjectSource> getDescriptor() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }

        Descriptor descriptor = jenkins.getDescriptorOrDie(getClass());
        return (Descriptor<PRQAFileProjectSource>) descriptor;
    }

    /**
     * PRQAReportSource
     * All registered {@link PostBuildActionSetup}s.
     */
    public static DescriptorExtensionList<PRQAFileProjectSource, PRQAFileProjectSourceDescriptor<PRQAFileProjectSource>> all() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }

        return jenkins.getDescriptorList(PRQAFileProjectSource.class);
    }

    public static List<PRQAFileProjectSourceDescriptor<?>> getDescriptors() {
        List<PRQAFileProjectSourceDescriptor<?>> descriptors = new ArrayList<>();

        DescriptorExtensionList<PRQAFileProjectSource, PRQAFileProjectSourceDescriptor<PRQAFileProjectSource>> all = all();
        if (all != null) {
            descriptors.addAll(all);
        }

        return descriptors;
    }

}
