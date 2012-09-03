package net.praqma.jenkins.plugin.prqa;

import hudson.model.Actionable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.jenkins.plugin.prqa.notifier.Messages;
import net.praqma.prga.excetions.PrqaException;
import net.praqma.prqa.PRQACommandLineUtility;
import net.praqma.prqa.products.QAV;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;
import org.apache.commons.io.FileUtils;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus> {
    
    private QAV qaverify;
    
    /**
     * Method that deletes old files in workspace on slaves. 
     * 
     */ 
    private int deleteOldLogFiles(String root, String... fileNames) throws IOException, InterruptedException {
        int deleteCount = 0;
        for(String name : fileNames) {
            String fullPath = root + PRQACommandLineUtility.FILE_SEPARATOR + name;
            listener.getLogger().println(Messages.PRQARemote_AttemptDelete(fullPath));
            //listener.getLogger().println(String.format("Attempting to delete old log file: %s", fullPath));
            if(FileUtils.deleteQuietly(new File(fullPath))) {
                //listener.getLogger().println(String.format("Succesfully deleted old log file: %s", fullPath));
                listener.getLogger().println(Messages.PRQARemote_SuccesfulDelete(fullPath));
                deleteCount++;
            }
        }
        return deleteCount;
    }
    
    
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
            
            //Replacing %WORKSPACE% with remote file system workspace path. 
            String projFile = report.getReportTool().getProjectFile(); 
            String replaced = projFile.replace("%WORKSPACE%", file.getPath());
            report.getReportTool().setProjectFile(replaced);
            setup(file.getPath(), PRQAReport.XHTML); 
            PRQAComplianceStatus status = null;            
            /**
             * Clean up
             */
            int deleteCount = deleteOldLogFiles(file.getPath(),"qavupload.log","qaimport.log");
            listener.getLogger().println(Messages.PRQARemote_SuccesXNumDelete(new Integer(deleteCount)));
            
            
            if(generateReports) {
                listener.getLogger().println(Messages.PRQARemote_QARVersion());
                listener.getLogger().println(report.getReportTool().getProductVersion());
                listener.getLogger().println(Messages.PRQARemote_AnalyzeWithTool());
                listener.getLogger().println(report.getReportTool().getAnalysisTool().getProductVersion());
                listener.getLogger().println(Messages.PRQARemote_ExecutingCommand());
                listener.getLogger().println(report.getReportTool().getCommand());
                status = report.generateReport();
            } else {
                listener.getLogger().println(Messages.PRQARemote_Disabled());
            }

            if(qaverify != null) {
                listener.getLogger().println(Messages.PRQARemote_BeginUploadProc());
                String command = qaverify.qavUpload(file.getPath(), generateReports);
                listener.getLogger().println(Messages.PRQARemote_ExecuteUploadCommand());
                listener.getLogger().println(command);
                qaverify.generateUpload(command, file.getPath(), generateReports);
                listener.getLogger().println(Messages.PRQARemote_QAVUploadSucces());
            }
            return status;
        } catch (PrqaException ex) {
            if(report.getCmdResult() != null) {
                for(String error : report.getCmdResult().errorList) {
                    listener.getLogger().println(error);
                }
            }
            throw new IOException(String.format("Failed in PRQARemoteComplianceReport with message(%s)\n%s",ex.getClass().getSimpleName(),ex.getMessage()),ex);
        } finally {
            if(!silentMode) {
                if(report.getCmdResult() != null) {
                    for(String outline : report.getCmdResult().stdoutList) {
                        listener.getLogger().println(outline);
                    }
                }
            }
            listener.getLogger().println(Messages.PRQARemote_FinishSucces());
        } 
    }
}
