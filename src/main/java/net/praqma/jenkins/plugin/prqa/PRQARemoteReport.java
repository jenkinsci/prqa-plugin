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
import java.util.HashMap;
import java.util.Map;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAReportSettings;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.util.execute.CmdResult;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Praqma
 */
public class PRQARemoteReport implements FileCallable<PRQAComplianceStatus>{

	private static final long serialVersionUID = 1L;
	
    private PRQAReport report;
    private BuildListener listener;
    boolean isUnix;
 
    public PRQARemoteReport(PRQAReport report, BuildListener listener, boolean isUnix) {
        this.report = report;
        this.listener = listener;
        this.isUnix = isUnix;
    }
    
    private HashMap<String,String> expandEnvironment(HashMap<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSetting) {
        String pathVar = "path";
        Map<String,String> localEnv = System.getenv();


        for(String s : localEnv.keySet()) {
            if(s.equalsIgnoreCase(pathVar)) {
                pathVar = s;
                break;
            }
        }
        
        String currentPath = localEnv.get(pathVar);

        String delimiter = System.getProperty("file.separator");
        String pathSep = System.getProperty("path.separator");
        
        if(environment != null) {
            if(reportSetting.product.equalsIgnoreCase("qac")) {
                String slashPath = PRQAApplicationSettings.addSlash(environment.get("QACPATH"), delimiter);
                environment.put("QACPATH", slashPath);       

                String qacBin = PRQAApplicationSettings.addSlash(environment.get("QACPATH"), delimiter) + "bin";
                environment.put("QACBIN", qacBin);
                environment.put("QACHELPFILES", environment.get("QACPATH") + "help");

                currentPath = environment.get("QACBIN") + pathSep + currentPath;
                environment.put("QACTEMP", System.getProperty("java.io.tmpdir"));                
            } else {
                String slashPath = PRQAApplicationSettings.addSlash(environment.get("QACPPPATH"), delimiter);
                environment.put("QACPPPATH", slashPath);

                String qacppBin = PRQAApplicationSettings.addSlash(environment.get("QACPPPATH"), delimiter) + "bin";
                environment.put("QACPPBIN", qacppBin);
                environment.put("QACPPHELPFILES", environment.get("QACPPPATH") + "help");

                currentPath = environment.get("QACPPBIN") + pathSep + currentPath;
                environment.put("QACPPTEMP", System.getProperty("java.io.tmpdir"));

            }
            
            currentPath = PRQAApplicationSettings.addSlash(appSettings.qarHome, delimiter) + "bin" + pathSep + currentPath;
            if(isUnix) {
                currentPath = PRQAApplicationSettings.addSlash(appSettings.qavClientHome, delimiter) + "bin" + pathSep + currentPath;
            } else {
                currentPath = appSettings.qavClientHome + pathSep + currentPath;
            }
            currentPath = PRQAApplicationSettings.addSlash(appSettings.qawHome, delimiter) + "bin" + pathSep + currentPath;
            environment.put(pathVar, currentPath);
            
        }
        return environment;
        
    }
    
    @Override
    public PRQAComplianceStatus invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {        
        try {
            
            HashMap<String,String> expandedEnvironment = expandEnvironment(report.getEnvironment(), report.getAppSettings(), report.getSettings());

            report.setEnvironment(expandedEnvironment);
            report.setWorkspace(f);

            
            
            /**
             * If the project file is null at this point. It means that this is a report based on a settings file.
             * 
             * We skip the analysis phase
             */
            if(!StringUtils.isBlank(report.getSettings().projectFile)) {
                listener.getLogger().println("Analysis command:");
                listener.getLogger().println(report.createAnalysisCommand(isUnix));
                report.analyze(isUnix);
            }
            
            listener.getLogger().println("Report command:");
            listener.getLogger().println(report.createReportCommand(isUnix));
            report.report(isUnix);
            
            if(!StringUtils.isBlank(report.createUploadCommand())) {
                listener.getLogger().println("Uploading with command:");
                listener.getLogger().println(report.createUploadCommand());
                CmdResult uploadResult = report.upload();
            }
            
            return report.getComplianceStatus();
        } catch (PrqaException exception) {
            throw new IOException(exception.getMessage(), exception);
        } 
    }
    
}
