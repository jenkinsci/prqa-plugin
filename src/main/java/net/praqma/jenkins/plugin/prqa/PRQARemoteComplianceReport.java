package net.praqma.jenkins.plugin.prqa;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.prqa.products.QAV;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus> {
    
    private QAV qaverify;
    
    public PRQARemoteComplianceReport (PRQAReport<?> report, BuildListener listener, boolean silentMode) {
        super(report,listener,silentMode);
    }
    
    public PRQARemoteComplianceReport (PRQAReport<?> report, BuildListener listener, boolean silentMode, QAV qaverify) {
        super(report,listener,silentMode);
        this.qaverify = qaverify;
    }
    
    @Override
    public PRQAComplianceStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        try {    
            setup(file.getPath(), PRQAReport.XHTML);
            out.println("Using Qar version: ");
            out.println(report.getReportTool().getProductVersion());
            out.println("Analyzing with tool: ");
            out.println(report.getReportTool().getAnalysisTool().getProductVersion());
            
            //Print actual command
            out.println("Executing command:");
            out.println(report.getReportTool().getCommand());
            PRQAComplianceStatus status = report.generateReport();
            //public String upload(String snapshotName, String qavOutputPath, String projectLocation, String uploadProjectName) throws PrqaException { 
            if(qaverify != null) {
                out.println(report.qavImport(file.getPath()));
                out.println(report.upload(file.getPath()));
                out.println("====NEW COMMANDS====");
                out.println(qaverify.qavImport(file.getPath()));
                out.println(qaverify.qavUpload(file.getPath()));
                out.println("====END NEW COMMANDS===");
            }

            
            
            return status;
        } catch (PrqaException ex) {
            if(report.getCmdResult() != null) {
                for(String error : report.getCmdResult().errorList) {
                    out.println(error);
                }
            }
            throw new IOException(ex);
        } finally {
            if(!silentMode) {
                if(report.getCmdResult() != null) {
                    for(String outline : report.getCmdResult().stdoutList) {
                        out.println(outline);
                    }
                }
            }
        } 
    }
}
