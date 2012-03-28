package net.praqma.jenkins.plugin.prqa;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.reports.PRQACodeReviewReport;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQACodeReviewStatus;

/**
 *
 * @author Praqma
 */
public class PRQARemoteCodeReviewReport extends PRQARemoteReporting<PRQACodeReviewStatus,PRQACodeReviewReport> {

    public PRQARemoteCodeReviewReport(PRQACodeReviewReport report, BuildListener listener, boolean silentMode) {
        super(report, listener, silentMode);
    }
    
    @Override
    public PRQACodeReviewStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        setup(file.getPath(), PRQAReport.XHTML);
        try {
            listener.getLogger().println(String.format("Beginning report generation with the follwoing command:\n %s",report.getQar().getCommand()));
            return report.completeTask();
        } catch (PrqaException ex) {
            listener.getLogger().println("Failed executing command: "+report.getQar().getBuilder().getCommand());
            throw new IOException(ex);
        } 
    }
    
}
