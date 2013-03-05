/*
 * The MIT License
 *
 * Copyright 2013 Praqma.
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

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import net.praqma.jenkins.plugin.prqa.setup.PRQAToolSuite;
import net.praqma.prga.excetions.PrqaException;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.reports.PRQAReport2;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.util.execute.CmdResult;

/**
 *
 * @author Praqma
 */
public class PRQARemoteReport implements FileCallable<PRQAComplianceStatus>{

    private PRQAReport2 report;
    private BuildListener listener;
 
    public PRQARemoteReport(PRQAReport2 report, BuildListener listener) {
        this.report = report;
        this.listener = listener;     
        
    }
    
    @Override
    public PRQAComplianceStatus invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {        
        try {

            String pathVar = "path";
            Map<String,String> environment = System.getenv();
            

            for(String s : environment.keySet()) {
                if(s.equalsIgnoreCase(pathVar)) {
                    pathVar = s;
                    break;
                }
            }
            
            String currentPath = environment.get(pathVar);

            String delimiter = System.getProperty("file.separator");
            if(report.getSettings().product.equalsIgnoreCase("qac")) {
                String slashPath = PRQAApplicationSettings.addSlash(report.getEnvironment().get("QACPATH"), delimiter);
                report.getEnvironment().put("QACPATH", slashPath);       
 
                String qacBin = PRQAApplicationSettings.addSlash(report.getEnvironment().get("QACPATH"), delimiter) + "bin";
                report.getEnvironment().put("QACBIN", qacBin);
                report.getEnvironment().put("QACHELPFILES", report.getEnvironment().get("QACPATH") + "help");
                
                currentPath = report.getEnvironment().get("QACBIN") + ";" + currentPath;
                report.getEnvironment().put("QACTEMP", System.getProperty("java.io.tmpdir"));
                
           
            } else {
                String slashPath = PRQAApplicationSettings.addSlash(report.getEnvironment().get("QACPPPATH"), delimiter);
                report.getEnvironment().put("QACPPPATH", slashPath);
                
                String qacppBin = PRQAApplicationSettings.addSlash(report.getEnvironment().get("QACPPPATH"), delimiter) + "bin";
                report.getEnvironment().put("QACPPBIN", qacppBin);
                report.getEnvironment().put("QACPPHELPFILES", report.getEnvironment().get("QACPPPATH") + "help");
                
                currentPath = report.getEnvironment().get("QACPPBIN") + ";" + currentPath;
                report.getEnvironment().put("QACPPTEMP", System.getProperty("java.io.tmpdir"));

            }
            
            currentPath = PRQAApplicationSettings.addSlash(report.getAppSettings().qarHome, delimiter) + "bin;" + currentPath;
            currentPath = report.getAppSettings().qavClientHome + ";" + currentPath;
            currentPath = PRQAApplicationSettings.addSlash(report.getAppSettings().qawHome, delimiter) + "bin;" + currentPath;
            
            report.getEnvironment().put(pathVar, currentPath);
 
            report.setWorkspace(f);
            listener.getLogger().println("===Printing Environment===");
            for(String s : report.printEnvironmentAsFromPJUTils()) {
                listener.getLogger().println(s);
            }
            listener.getLogger().println("===Printing Environment===");
            
            listener.getLogger().println("Analysis command:");
            listener.getLogger().println(report.createAnalysisCommand());
            report.analyze();
            
            listener.getLogger().println("Reports command:");
            listener.getLogger().println(report.createReportCommand());
            report.report();
            
            listener.getLogger().println("Upload command:");
            listener.getLogger().println(report.createUploadCommand());
            CmdResult uploadResult = report.upload();
            if(uploadResult == null) {
                listener.getLogger().println("No QAVerify upload selected");
            }
            
            return report.getComplianceStatus();
        } catch (PrqaException exception) {
            throw new IOException("Failed to obtain compliance status", exception);
        } catch (Exception ex) {
            throw new IOException( String.format( "Caught exception of type %s",ex.getClass().getName() ), ex);
        }
    }
    
}
