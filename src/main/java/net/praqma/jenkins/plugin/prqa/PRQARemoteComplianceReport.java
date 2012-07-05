package net.praqma.jenkins.plugin.prqa;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus> {
    
    public PRQARemoteComplianceReport (PRQAReport<?> report, BuildListener listener, boolean silentMode) {
        super(report,listener,silentMode);
    }
    
    @Override
    public PRQAComplianceStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        try {    
            setup(file.getPath(), PRQAReport.XHTML);     
            return report.generateReport();
        } catch (PrqaException ex) {
            if(report.getCmdResult() != null) {
                for(String error : report.getCmdResult().errorList) {
                    listener.getLogger().println(error);
                }
            }
            throw new IOException(ex);
        } finally {
            if(!silentMode) {
                if(report.getCmdResult() != null) {
                    for(String outline : report.getCmdResult().stdoutList) {
                        listener.getLogger().println(outline);
                    }
                }
            }
        } 
    }
}
