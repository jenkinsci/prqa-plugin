package net.praqma.jenkins.plugin.prqa;

import hudson.model.Actionable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.products.QAV;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus> {
    
    private QAV qaverify;    
    public PRQARemoteComplianceReport (PRQAReport<?> report, BuildListener listener, boolean silentMode, Actionable a, boolean skip) {
        super(report,listener,silentMode, a, skip);
    }
    
    public PRQARemoteComplianceReport (PRQAReport<?> report, BuildListener listener, boolean silentMode, Actionable a, QAV qaverify, boolean skip) {
        super(report,listener,silentMode, a, skip);
        this.qaverify = qaverify;
    }
    
    
    @Override
    public PRQAComplianceStatus perform(File file, VirtualChannel vc) throws IOException, InterruptedException {        
        try {
           
            setup(file.getPath(), PRQAReport.XHTML); 
            PRQAComplianceStatus status = null;
            
            if(generateReports) {
                listener.getLogger().println("Using Qar version: ");
                listener.getLogger().println(report.getReportTool().getProductVersion());
                listener.getLogger().println("Analyzing with tool: ");
                listener.getLogger().println(report.getReportTool().getAnalysisTool().getProductVersion());

                listener.getLogger().println("Executing command:");
                listener.getLogger().println(report.getReportTool().getCommand());
  
                status = report.generateReport();
            } else {
                listener.getLogger().println("No report generation selected no - QAR commands being run");
            }

            if(qaverify != null) {
                listener.getLogger().println(qaverify.qavImport(file.getPath()));
                listener.getLogger().println(qaverify.qavUpload(file.getPath()));
            }
            
            return status;
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

            listener.getLogger().println("Finished remote reporting.");
        } 
    }
}
