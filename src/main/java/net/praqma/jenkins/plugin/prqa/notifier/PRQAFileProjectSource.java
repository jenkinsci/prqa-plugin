package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;

public class PRQAFileProjectSource implements Describable<PRQAFileProjectSource>, ExtensionPoint {

    @Override
    public Descriptor<PRQAFileProjectSource> getDescriptor() {
        return (Descriptor<PRQAFileProjectSource>) Jenkins.getInstance().getDescriptorOrDie( getClass() );
    }
    
    /**PRQAReportSource
    * All registered {@link PostBuildActionSetup}s.
    */
   public static DescriptorExtensionList<PRQAFileProjectSource, PRQAFileProjectSourceDescriptor<PRQAFileProjectSource>> all() {
           return Jenkins.getInstance().<PRQAFileProjectSource, PRQAFileProjectSourceDescriptor<PRQAFileProjectSource>> getDescriptorList( PRQAFileProjectSource.class );
   }
   
   public static List<PRQAFileProjectSourceDescriptor<?>> getDescriptors() {
       List<PRQAFileProjectSourceDescriptor<?>> descriptors = new ArrayList<PRQAFileProjectSourceDescriptor<?>>();
       for(PRQAFileProjectSourceDescriptor<?> desc : all()) {
           descriptors.add(desc);
       }
       
       return descriptors;
   }
    
}