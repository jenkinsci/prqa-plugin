/*
 * The MIT License
 *
 * Copyright 2012 jes.
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

import net.praqma.prqa.PRQAContext.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jes
 */
public class Config {

    public static List<String> getReports() {
        List<String> list = new ArrayList<String>();
        for (QARReportType report : QARReportType.values()) {
            if (report.equals(QARReportType.Compliance)) {
                list.add(report.toString());
            }
        }
        return list;
    }
    public static final String COMPLIANCE_REPORT_PATH = "C:\\Projects\\PRQA-plugin\\Compliance_Report.xhtml";
}
