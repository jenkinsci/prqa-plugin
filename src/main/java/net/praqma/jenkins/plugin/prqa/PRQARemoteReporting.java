package net.praqma.jenkins.plugin.prqa;

import hudson.FilePath;
import hudson.model.BuildListener;
import net.praqma.prqa.PRQAStatus;
import net.praqma.prqa.reports.PRQAReport;

/**
 * @author Praqma
 */
public abstract class PRQARemoteReporting<K extends PRQAStatus, T extends PRQAReport> implements FilePath.FileCallable<K> {    
    protected BuildListener listener;
    protected boolean silentMode;
    protected T report;

    public PRQARemoteReporting (T report, BuildListener listener, boolean silentMode) {
        this.report = report;
        this.listener = listener;
        this.silentMode = silentMode;
    }
}
