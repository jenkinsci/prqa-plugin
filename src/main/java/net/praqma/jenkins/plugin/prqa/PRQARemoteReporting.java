package net.praqma.jenkins.plugin.prqa;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.PRQAComplianceStatus;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQAComplianceReport;
import net.praqma.prqa.reports.PRQAReport;

/**
 *
 * @author Praqma
 */
public class PRQARemoteReporting implements FilePath.FileCallable<PRQAComplianceStatus> {
    
    private QAR qar;
    private BuildListener listener;
    
    //Silent mode for 
    private boolean silentMode;
    
    /**
     * 
     * @param qar The command line wrapper for the Programming Reseach QAR tool
     * @param listener Jenkins build listener, for writing console putut.  
     */
    public PRQARemoteReporting(QAR qar, BuildListener listener, boolean silentMode) {
        this.qar = qar;
        this.listener = listener;
        this.silentMode = silentMode;
    }
       
    public PRQARemoteReporting(String command, String productHomeDir, BuildListener listener) {
        this.qar = new QAR(productHomeDir, command);
        this.listener = listener;
    }

    @Override
    public PRQAComplianceStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        try {
            qar.setReportOutputPath(file.getPath());
            qar.setCommandBase(file.getPath());

            String qarEmbedded = "qar %Q %P+ %L+ "+ PRQACommandBuilder.getReportTypeParameter(qar.getType().toString()) + " " +
            PRQACommandBuilder.getProjectName() + " " + PRQACommandBuilder.getOutputPathParameter(file.getPath(),true) + " " + PRQACommandBuilder.getViewingProgram("dummy") 
            + " " + PRQACommandBuilder.getReportFormatParameter(PRQAReport.XHTML, false);
        
            if(!silentMode)
                qar.getBuilder().appendArgument("-plog");
            
            qar.getBuilder().appendArgument(PRQACommandBuilder.getMaseq(qarEmbedded));           
            qar.setCommand(qar.getBuilder().getCommand());
                                   
            PRQAComplianceReport prreport = new PRQAComplianceReport<PRQAComplianceStatus,String>(qar);
            
            return prreport.completeTask();
        } catch (PrqaException ex) {
            listener.getLogger().println("Failed executing command: "+qar.getBuilder().getCommand());
            throw new IOException(ex);
        } finally {
            listener.getLogger().println("Finished remote reporting.");
        } 
    }
    
}
