/*
 * The MIT License
 *
 * Copyright 2014 RINF.
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
import java.util.Collections;
import java.util.Map;

import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.QaFrameworkVersion;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.products.QACli;
import net.praqma.prqa.reports.QAFrameworkReport;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.util.execute.CmdResult;
import net.prqma.prqa.qaframework.QaFrameworkReportSettings;

import org.apache.commons.lang.StringUtils;
import org.jdom2.JDOMException;

public class QAFrameworkRemoteReport implements FileCallable<PRQAComplianceStatus> {

    private static final long serialVersionUID = 1L;

    private QAFrameworkReport report;
    private BuildListener listener;
    boolean isUnix;
    private QaFrameworkReportSettings reportSetting;

    public QAFrameworkRemoteReport(QAFrameworkReport report, BuildListener listener, boolean isUnix) {
        this.report = report;
        this.listener = listener;
        this.isUnix = isUnix;
    }

    private Map<String, String> expandEnvironment(Map<String, String> environment, PRQAApplicationSettings appSettings,
            QaFrameworkReportSettings reportSetting) {
        this.reportSetting = reportSetting;
        if (environment == null) {
            return Collections.emptyMap();
        }
        String delimiter = System.getProperty("file.separator");
        environment.put(QACli.QAF_BIN_PATH,
                PRQAApplicationSettings.addSlash(environment.get(QACli.QAF_INSTALL_PATH), delimiter) + "common"
                + delimiter + "bin");
        return environment;
    }

    @Override
    public PRQAComplianceStatus invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

        Map<String, String> expandedEnvironment = expandEnvironment(report.getEnvironment(), report.getAppSettings(),
                report.getSettings());

        report.setEnvironment(expandedEnvironment);
        report.setWorkspace(f);
        
        PrintStream out = listener.getLogger();

        //out.println("Workspace form invoke:"+f);
        out.println("Workspace form invoke:" + f.getAbsolutePath());
        //out.println("Workspace form invoke:"+f.getCanonicalPath());

        /**
         * If the project file is null at this point. It means that this is a
         * report based on a settings file.
         *
         * We skip the analysis phase
         */
        try {
            if (StringUtils.isBlank(report.getSettings().getQaInstallation())) {
                throw new PrqaException("Incorrect configuration!");
            }

            CmdResult pullUnifyProject = report.pullUnifyProjectQacli(isUnix, out);
            logCmdResult(pullUnifyProject, out);

            CmdResult analyzeResult = report.analyzeQacli(isUnix, out);
            logCmdResult(analyzeResult, out);

            CmdResult cmaAnalysisResult = report.cmaAnalysisQacli(isUnix, out);
            logCmdResult(cmaAnalysisResult, out);
            
            if (reportSetting.isGenCrReport()) {
                String Report = "CRR";
                CmdResult crrGenerationResult = report.reportQacli(isUnix, Report, out);
                logCmdResult(crrGenerationResult, out);
            }
            if (reportSetting.isGenMdReport()) {
                String Report = "MDR";
                CmdResult mdrGenerationResult = report.reportQacli(isUnix, Report, out);
                logCmdResult(mdrGenerationResult, out);
            }
            if (reportSetting.isGenSupReport()) {
                String Report = "SR";
                CmdResult srGenerationResult = report.reportQacli(isUnix, Report, out);
                logCmdResult(srGenerationResult, out);
            }
            String Report = "RCR";
            CmdResult rcrGenerationResult = report.reportQacli(isUnix, Report, out);
            logCmdResult(rcrGenerationResult, out);
            
            if (reportSetting.isPublishToQAV()) {
                CmdResult uploadResult = report.uploadQacli(out);
                logCmdResult(uploadResult, out);
            }
            
            return report.getComplianceStatus(out);
        } catch (PrqaException exception) {
            throw new IOException(exception.getMessage(), exception);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private void logCmdResult(CmdResult result, PrintStream out) {
        if (result == null) {
            return;
        }
        out.println(result.stdoutBuffer.toString());
    }

    public void setQaFrameworkVersion(QaFrameworkVersion qaFrameworkVersion) {
        report.setQaFrameworkVersion(qaFrameworkVersion);
    }
}
