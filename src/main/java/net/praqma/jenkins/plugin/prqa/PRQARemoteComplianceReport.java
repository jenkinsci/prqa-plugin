package net.praqma.jenkins.plugin.prqa;

import hudson.model.Actionable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.reports.PRQAComplianceReport;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus,PRQAComplianceReport> {
    
    public PRQARemoteComplianceReport (PRQAComplianceReport report, BuildListener listener, boolean silentMode, Actionable a) {
        super(report,listener,silentMode, a);
    }
    
    @Override
    public PRQAComplianceStatus perform(File file, VirtualChannel vc) throws IOException, InterruptedException {        
        try {
            setup(file.getPath(), PRQAReport.XHTML);
            listener.getLogger().println(String.format("Beginning report generation with the following command:\n %s",report.getQar().getCommand()));
            return report.completeTask();
        } catch (PrqaException ex) {
            listener.getLogger().println("Failed executing command: "+report.getQar().getBuilder().getCommand());
            if(report.getCmdResult() != null) {
                for(String error : report.getCmdResult().errorList) {
                    listener.getLogger().println(error);
                }
            }
            throw new IOException(ex);
        } finally {
            if(report.getCmdResult() != null) {
                for(String outline : report.getCmdResult().stdoutList) {
                    listener.getLogger().println(outline);
                }
            }
            listener.getLogger().println("Finished remote reporting.");
        } 
    }
}
