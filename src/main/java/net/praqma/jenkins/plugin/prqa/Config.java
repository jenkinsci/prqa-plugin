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
            if (report.equals(QARReportType.Compliance) || report.equals(QARReportType.Quality)) {
                list.add(report.toString());
            }
        }
        return list;
    }
    
    public static final String LOGO_PATH = "/plugins/prqa-plugin/prqa_logo.png";
}