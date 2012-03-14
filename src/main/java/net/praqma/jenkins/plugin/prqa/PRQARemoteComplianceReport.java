package net.praqma.jenkins.plugin.prqa;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.PRQAComplianceStatus;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.reports.PRQAComplianceReport;
import net.praqma.prqa.reports.PRQAReport;

/**
 * The compliance report. 
 * @author Praqma
 */
public class PRQARemoteComplianceReport extends PRQARemoteReporting<PRQAComplianceStatus,PRQAComplianceReport> {
    
    public PRQARemoteComplianceReport (PRQAComplianceReport report, BuildListener listener, boolean silentMode) {
        super(report,listener,silentMode);
    }
    
    @Override
    public PRQAComplianceStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {        
        try {
            report.getQar().setReportOutputPath(file.getPath());
            report.getQar().setCommandBase(file.getPath());

            String qarEmbedded = "qar %Q %P+ %L+ "+ PRQACommandBuilder.getReportTypeParameter(report.getQar().getType().toString()) + " " +
            PRQACommandBuilder.getProjectName() + " " + PRQACommandBuilder.getOutputPathParameter(file.getPath(),true) + " " + PRQACommandBuilder.getViewingProgram("dummy") 
            + " " + PRQACommandBuilder.getReportFormatParameter(PRQAReport.XHTML, false);
        
            if(!silentMode)
                report.getQar().getBuilder().appendArgument("-plog");
            report.getQar().getBuilder().appendArgument(PRQACommandBuilder.getMaseq(qarEmbedded));           
            report.getQar().setCommand(report.getQar().getBuilder().getCommand());
           
            listener.getLogger().println(String.format("Beginning report generation with the follwoing command:\n %s",report.getQar().getCommand()));
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
