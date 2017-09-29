/*
 * The MIT License
 *
 * Copyright 2015 Programming Research.
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

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.products.QACli;
import net.praqma.prqa.reports.QAFrameworkReport;
import net.prqma.prqa.qaframework.QaFrameworkReportSettings;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class QAFrameworkRemoteReportUpload
        extends MasterToSlaveFileCallable<Void>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private QAFrameworkReport report;
    private BuildListener listener;
    private QaFrameworkReportSettings reportSetting;

    public QAFrameworkRemoteReportUpload(QAFrameworkReport report,
                                         BuildListener listener) {
        this.report = report;
        this.listener = listener;
    }

    private Map<String, String> expandEnvironment(Map<String, String> environment,
                                                  QaFrameworkReportSettings reportSetting) {
        this.reportSetting = reportSetting;
        if (environment == null) {
            return Collections.emptyMap();
        }
        environment.put(QACli.QAF_BIN_PATH, PRQAApplicationSettings.addSlash(environment.get(QACli.QAF_INSTALL_PATH),
                                                                             File.separator) + "common" + File.separator + "bin");
        return environment;
    }

    @Override
    public Void invoke(File f,
                       VirtualChannel channel)
            throws IOException, InterruptedException {

        Map<String, String> expandedEnvironment = expandEnvironment(report.getEnvironment(), report.getSettings());

        report.setEnvironment(expandedEnvironment);
        report.setWorkspace(f);

        PrintStream out = listener.getLogger();
        /*
          If the project file is null at this point. It means that this is a
          report based on a settings file.

          We skip the analysis phase

         */
        try {
            if (StringUtils.isBlank(report.getSettings()
                                          .getQaInstallation())) {
                throw new PrqaException("Incorrect configuration of QA framework installation!");
            }
            if (reportSetting.isLoginToQAV() && reportSetting.isPublishToQAV()) {
                report.uploadQacli(out);
            }
            return null;
        } catch (PrqaException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
