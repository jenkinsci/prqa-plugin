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
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAReportSettings;
import net.praqma.prqa.exceptions.PrqaSetupException;
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
    
    private Map<String,String> expandEnvironment(Map<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSetting) throws PrqaSetupException {
        return PRQARemoteToolCheck.expandEnvironment(environment, appSettings, reportSetting, isUnix);
    }
    
    @Override
    public PRQAComplianceStatus invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {        
        try {
            Map<String,String> expandedEnvironment = expandEnvironment(report.getEnvironment(), report.getAppSettings(), report.getSettings());

            report.setEnvironment(expandedEnvironment);
            report.setWorkspace(f);

            PrintStream log = listener.getLogger();
            boolean reportBasedOnSettingFile = StringUtils.isBlank(report.getSettings().projectFile) && StringUtils.isBlank(report.getSettings().fileList);
            if(!reportBasedOnSettingFile) {
                log.println("Analysis command:");
                log.println(report.createAnalysisCommand(isUnix));
                log.println(report.analyze(isUnix).stdoutBuffer);
            }
            
            log.println("Report command:");
            log.println(report.createReportCommand(isUnix));
            log.println(report.report(isUnix).stdoutBuffer);
            
            if(StringUtils.isNotBlank(report.createUploadCommand())) {
                log.println("Uploading with command:");
                log.println(report.createUploadCommand());
                log.println(report.upload().stdoutBuffer);
            }
            
            return report.getComplianceStatus();
        } catch (PrqaException exception) {
            throw new IOException(exception.getMessage(), exception);
        } 
    }
    
}
