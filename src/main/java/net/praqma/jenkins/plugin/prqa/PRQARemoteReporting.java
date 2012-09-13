package net.praqma.jenkins.plugin.prqa;

import hudson.model.Actionable;
import hudson.model.BuildListener;
import net.praqma.logging.LoggingFileCallable;
import net.praqma.prqa.PRQAContext;
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
    protected boolean generateReports;


    public PRQARemoteReporting (PRQAReport<?> report, BuildListener listener, boolean silentMode, Actionable a, boolean skip) {
        super(a);
        this.report = report;
        this.listener = listener;
        this.silentMode = silentMode;
        generateReports = skip;
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
        
        //RQ-1
        if(report.isEnableDependencyMode()) {
            builder.appendArgument("-mode depend");
       }
        
        builder.appendArgument(PRQACommandBuilder.getDataFlowAnanlysisParameter(report.isEnableDataFlowAnalysis()));
        
        String reports = "";
        for (PRQAContext.QARReportType type : report.getChosenReports()) {
            reports += "qar %Q %P+ %L+ " + PRQACommandBuilder.getReportTypeParameter(type.toString(), true)+ " ";
            reports += PRQACommandBuilder.getViewingProgram("noviewer")+ " ";
            reports += PRQACommandBuilder.getReportFormatParameter(outputFormat, false)+ " ";
            reports += PRQACommandBuilder.getProjectName()+ " ";
            reports += PRQACommandBuilder.getOutputPathParameter(path, true);
            reports += "#";
        }
        
        //Remove trailing #
        reports = reports.substring(0, reports.length()-1);
        
        String qarEmbedded = (report.isUseCrossModuleAnalysis() ? "pal %Q %P+ %L+#" : "")+reports;

        builder.appendArgument(PRQACommandBuilder.getMaseq(qarEmbedded));
        report.getReportTool().setCommand(builder.getCommand());       
    }

    /**
     * @return the generateReports
     */
    public boolean isSkipReportGeneration() {
        return generateReports;
    }

    /**
     * @param generateReports the generateReports to set
     */
    public void setSkipReportGeneration(boolean skipReportGeneration) {
        this.generateReports = skipReportGeneration;
    }
}
