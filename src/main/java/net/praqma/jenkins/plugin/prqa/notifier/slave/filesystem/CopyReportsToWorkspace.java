package net.praqma.jenkins.plugin.prqa.notifier.slave.filesystem;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static net.praqma.prqa.reports.ReportType.*;

public class CopyReportsToWorkspace extends MasterToSlaveFileCallable<Boolean> implements Serializable {

    private final String qaProject;

    public CopyReportsToWorkspace(String qaProject) {
        this.qaProject = qaProject;
    }

    @Override
    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

        final String reportsPath = "prqa/reports";
        File qaFReports;

        if (StringUtils.isEmpty(qaProject)) {
            qaFReports = new File(reportsPath);
        } else {
            qaFReports = new File(qaProject + "/" + reportsPath);
        }

        if (!qaFReports.isDirectory()) {
            qaFReports = new File(f + "/" + reportsPath);
        }

        if (!qaFReports.isDirectory()) {
            return Boolean.FALSE;
        }

        File[] files = qaFReports.listFiles();
        assert files != null;

        for (File reportFile : files) {
            if (containsReportName(reportFile.getName())) {
                FileUtils.copyFileToDirectory(reportFile, f);
            }
        }

        return Boolean.TRUE;
    }

    private boolean containsReportName(String fileName) {
        return fileName.contains(CRR.name()) ||
                fileName.contains(SUR.name()) ||
                fileName.contains(RCR.name()) ||
                fileName.contains(MDR.name());
    }
}
