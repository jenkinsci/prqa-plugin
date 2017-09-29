package net.praqma.jenkins.plugin.prqa.notifier.slave.filesystem;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import net.praqma.jenkins.plugin.prqa.notifier.ReportFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class DeleteReportsFromWorkspace
        extends MasterToSlaveFileCallable<Boolean>
        implements Serializable {

    public DeleteReportsFromWorkspace() {
    }

    @Override
    public Boolean invoke(File f,
                          VirtualChannel channel)
            throws IOException, InterruptedException {

        File[] files = f.listFiles(new ReportFileFilter());

        if (files == null) {
            return Boolean.TRUE;
        }

        for (File report : files) {
            if (!report.delete()) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

}
