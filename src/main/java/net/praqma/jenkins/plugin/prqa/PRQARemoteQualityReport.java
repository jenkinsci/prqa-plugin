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

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAQualityStatus;

/**
 *
 * @author Praqma
 */
public class PRQARemoteQualityReport extends PRQARemoteReporting<PRQAQualityStatus> {

    @Override
    public PRQAQualityStatus invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        try {
            setup(file.getPath(), PRQAReport.XHTML);
            listener.getLogger().println(String.format("Beginning report generation with the follwoing command:\n %s",report.getReportTool().getCommand()));
            listener.getLogger().println(String.format("Using QAR Version %s", report.getReportTool().getProductVersion()));
            listener.getLogger().println(String.format("Using Analysis Tool %s", report.getReportTool().getAnalysisTool().getProductVersion()));
            return report.generateReport();
        } catch (PrqaException ex) {
            listener.getLogger().println("Failed executing command: "+((QAR)report.getReportTool()).getBuilder().getCommand());
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
    
    public PRQARemoteQualityReport(PRQAReport<?> report, BuildListener listener, boolean silentMode) {
        super(report,listener,silentMode);
    }
    
}
