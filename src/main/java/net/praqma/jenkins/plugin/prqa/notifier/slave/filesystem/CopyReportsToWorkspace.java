package net.praqma.jenkins.plugin.prqa.notifier.slave.filesystem;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.reports.QAFrameworkReport;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static net.praqma.prqa.reports.ReportType.CRR;
import static net.praqma.prqa.reports.ReportType.MDR;
import static net.praqma.prqa.reports.ReportType.RCR;
import static net.praqma.prqa.reports.ReportType.SUR;

public class CopyReportsToWorkspace extends MasterToSlaveFileCallable<Boolean> implements Serializable {

    private final String qaProject;

    public CopyReportsToWorkspace(String qaProject) {
        this.qaProject = qaProject;
    }

    @Override
    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

        final String reportsPath;
        try {
            reportsPath = QAFrameworkReport.extractReportsPath(f.getAbsolutePath(), qaProject);
        } catch (PrqaException e) {
            throw new IOException(e);
        }

        File[] files = new File(reportsPath).listFiles();
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
