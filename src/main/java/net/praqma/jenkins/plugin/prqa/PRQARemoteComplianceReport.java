package net.praqma.jenkins.plugin.prqa;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import net.praqma.prqa.PRQACommandLineUtility;
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
            
                    //TEST//
        
     
        
        String outpath = String.format(" -po qav::output=%s", file.getPath());
        
        String commandTest = "QAW qac \"C:\\Program Files (x86)\\PRQA\\QAC-8.0-R\\projects\\examples\\examples.prj\" ";
        commandTest+="C:\\Program\\ Files\\ (x86)\\PRQA\\QA\\ Verify\\ 1.3\\Client\\qaimport ";
        commandTest+="%Q  %F+ -sop %D -po qav::prqavcs=\"C:\\Program Files (x86)\\PRQA\\QA Verify 1.3\\Client\\vcs\\cvs.xml\""+outpath+" %P+\" –sfba";   
        
        out.println("Command (TEST)");
        out.println(commandTest);
        //TEST//
            
            
            
            return report.generateReport();
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
