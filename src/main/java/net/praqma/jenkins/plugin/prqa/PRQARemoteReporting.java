/*
 * The MIT License
 *
 * Copyright 2012 Praqma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.plugin.prqa;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import net.praqma.prqa.PRQAComplianceStatus;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQAComplianceReport;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.appenders.StreamAppender;
import org.apache.commons.io.FileUtils;

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
        File tmpCommand = null;
        StreamAppender sa = new StreamAppender(this.listener.getLogger());
        Logger.addAppender(sa);
        try {
            qar.setReportOutputPath(file.getPath());
            qar.setCommandBase(file.getPath());
            qar.getBuilder().appendArgument(PRQACommandBuilder.getOutputPathParameter(file.getPath()));
            qar.getBuilder().appendArgument(PRQACommandBuilder.getCmaf(file.getPath()+"\\qar_out"));
            
            //Create a temorary file holding the command. We did this because otherwise the command line interpreter won't accept quotes in commands. 
            tmpCommand = File.createTempFile("tempQAR", ".bat");
            FileWriter fw = new FileWriter(tmpCommand);
            PrintWriter pw = new PrintWriter(fw);
            
            pw.println("call \"C:\\Program Files (x86)\\PRQA\\QAC-8.0-R\\bin\\QACCONF.BAT\"");
            pw.println("call "+qar.getBuilder().getCommand());
            pw.close();
            
            
 
            qar.setCommand(tmpCommand.getPath());
            
            
            
            listener.getLogger().println(qar.getBuilder().getCommand());
            listener.getLogger().println(tmpCommand.getPath());
                        
            PRQAComplianceReport prreport = new PRQAComplianceReport<PRQAComplianceStatus,String>(qar);
            
            //TODO:Emulating the output(THIS SHOULD HAVE BEEN DONE BEFOREHAND BY QAC/QACPP
            //FileUtils.copyFile(new File(Config.COMPLIANCE_REPORT_PATH), new File(prreport.getFullReportPath()));
            //REMOVE

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
