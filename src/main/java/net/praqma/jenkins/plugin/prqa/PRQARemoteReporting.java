package net.praqma.jenkins.plugin.prqa;

import hudson.FilePath;
import hudson.model.BuildListener;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.products.PRQACommandBuilder;
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
    
    /*
     * Constructs the final command. Performs the setup.
     * 
     * Takes the workspace path and an output format for the report that is to be generated. The information about the type of report is already present here. 
     * 
     * So the task of this piece of code is to construct the final command for generating the report
     */
    protected void setup(String path, String outputFormat) {
        report.getQar().getBuilder().prependArgument(PRQACommandBuilder.getProduct(report.getQar().getProduct()));
        report.getQar().getBuilder().appendArgument(PRQACommandBuilder.getProjectFile(report.getQar().getProjectFile()));
        
        report.getQar().setReportOutputPath(path);
        report.getQar().setCommandBase(path);
        
        if(silentMode) {
            report.getQar().getBuilder().appendArgument("-plog");
        }
        
        String qarEmbedded = "qar %Q %P+ %L+ " + PRQACommandBuilder.getReportTypeParameter(report.getQar().getType().toString(),true) + " "
                    + PRQACommandBuilder.getProjectName() + " " + PRQACommandBuilder.getOutputPathParameter(path, true) + " " + PRQACommandBuilder.getViewingProgram("dummy")
                    + " " + PRQACommandBuilder.getReportFormatParameter(outputFormat, false);

        report.getQar().getBuilder().appendArgument(PRQACommandBuilder.getMaseq(qarEmbedded));
        report.getQar().setCommand(report.getQar().getBuilder().getCommand());       
    }
    
}
