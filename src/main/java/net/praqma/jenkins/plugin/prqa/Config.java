package net.praqma.jenkins.plugin.prqa;

import java.util.ArrayList;
import java.util.List;
import net.praqma.prqa.PRQAContext.QARReportType;

/**
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