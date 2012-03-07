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
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

/**
 *
 * @author Praqma
 */
public class PRQARemoteReporting implements FilePath.FileCallable<PRQAComplianceStatus> {
    
    private QAR qar;
    private BuildListener listener;
    
    /**
     * 
     * @param qar The command line wrapper for the Programming Reseach QAR tool
     * @param listener Jenkins build listener, for writing console putut.  
     */
    public PRQARemoteReporting(QAR qar, BuildListener listener) {
        this.qar = qar;
        this.listener = listener;
    }
       
    public PRQARemoteReporting(String command, String productHomeDir, BuildListener listener) {
        this.qar = new QAR(productHomeDir, command);
        this.listener = listener;
    }

    @Override
    public PRQAComplianceStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        StreamAppender sa = new StreamAppender(this.listener.getLogger());
        Logger.addAppender(sa);
        try {
            qar.setReportOutputPath(file.getPath());
            qar.setCommandBase(file.getPath());
            qar.getBuilder().appendArgument(PRQACommandBuilder.getOutputPathParameter(file.getPath()));
            qar.getBuilder().appendArgument(PRQACommandBuilder.getCmaf(file.getPath()+"\\qar_out"));
            
            qar.setCommand(qar.getBuilder().getCommand());
            listener.getLogger().println(qar.getBuilder().getCommand());
                                   
            PRQAComplianceReport prreport = new PRQAComplianceReport<PRQAComplianceStatus,String>(qar);
            
            return prreport.completeTask();
        } catch (PrqaException ex) {
            
            listener.getLogger().println("Failed executing command: "+qar.getCommand());
            listener.getLogger().println("Commandbuilder: "+ qar.getBuilder().getCommand());
            throw new IOException(ex);
        } finally {
            Logger.removeAppender(sa);
        } 
    }
    
}
