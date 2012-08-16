package net.praqma.jenkins.plugin.prqa;

import hudson.model.Actionable;
import hudson.model.BuildListener;
import net.praqma.logging.LoggingFileCallable;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAStatus;

/**
 * @author Praqma
 */

public abstract class PRQARemoteReporting<T extends PRQAStatus> extends LoggingFileCallable<T> {    
    protected BuildListener listener;
    protected boolean silentMode;
    protected PRQAReport<?> report;


    public PRQARemoteReporting (PRQAReport<?> report, BuildListener listener, boolean silentMode, Actionable a) {
        super(a);
        this.report = report;
        this.listener = listener;
        this.silentMode = silentMode;
    }
    
    /**
     * This is setup. Must be called before anything else in the perform() method of concrete implemntations. We need to do this to setup output paths for the report.
     * 
     * Constructs the final command. Performs the setup.
     * 
     * Takes the workspace path and an output format for the report that is to be generated. The information about the type of report is already present here. 
     * 
     * So the task of this piece of code is to construct the final command for generating the report
     **/
    protected void setup(String path, String outputFormat) {
        //Get the command builder. 
        PRQACommandBuilder builder = report.getReportTool().getBuilder();
        builder.prependArgument(PRQACommandBuilder.getProduct(report.getReportTool().getAnalysisTool()));
        builder.appendArgument(PRQACommandBuilder.getProjectFile(report.getReportTool().getProjectFile()));
        
        report.getReportTool().setReportOutputPath(path);
        report.getReportTool().setCommandBase(path);
        
        if(silentMode) {
            builder.appendArgument("-plog");
        }
        
        String qarEmbedded = (report.isUseCrossModuleAnalysis() ? "pal %Q %P+ %L+#" : "")+"qar %Q %P+ %L+ " + PRQACommandBuilder.getReportTypeParameter(report.getReportTool().getType().toString(),true) + " "
                    + PRQACommandBuilder.getProjectName() + " " + PRQACommandBuilder.getOutputPathParameter(path, true) + " " + PRQACommandBuilder.getViewingProgram("dummy")
                    + " " + PRQACommandBuilder.getReportFormatParameter(outputFormat, false);

        builder.appendArgument(PRQACommandBuilder.getMaseq(qarEmbedded));
        report.getReportTool().setCommand(builder.getCommand());       
    }
}
