/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.remoting.Future;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.praqma.jenkins.plugin.prqa.*;
import net.praqma.jenkins.plugin.prqa.graphs.*;
import net.praqma.prqa.PRQA;
import net.praqma.prqa.PRQAContext.AnalysisTools;
import net.praqma.prqa.PRQAContext.ComparisonSettings;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.status.StatusCategory;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {
    private PrintStream out;
    private List<PRQAGraph> graphTypes;
    private HashMap<StatusCategory,Number> thresholds;
    private boolean showThresholds;
    
    private Boolean totalBetter;
    private Integer totalMax;
    private String product;
    private QARReportType reportType;
 
    private String settingFileCompliance;
    private String settingMaxMessages;
    private String settingProjectCompliance;
    
    private Double fileComplianceIndex;
    private Double projectComplianceIndex;
    
    private String projectFile;

    @DataBoundConstructor
    public PRQANotifier(String reportType, String product, boolean totalBetter, String totalMax, String fileComplianceIndex, String projectComplianceIndex, String settingMaxMessages, String settingFileCompliance, String settingProjectCompliance, String projectFile) {
        this.reportType = QARReportType.valueOf(reportType.replaceAll(" ", ""));
        this.product = product;
        this.totalBetter = totalBetter;
        this.totalMax = parseIntegerNullDefault(totalMax);
        this.fileComplianceIndex = parseDoubleNullDefault(fileComplianceIndex);
        this.projectComplianceIndex = parseDoubleNullDefault(projectComplianceIndex);
        this.settingProjectCompliance = settingProjectCompliance;
        this.settingMaxMessages = settingMaxMessages;
        this.settingFileCompliance = settingFileCompliance;
        this.projectFile = projectFile;
        this.thresholds = new HashMap<StatusCategory, Number>();
 
        if(ComparisonSettings.valueOf(settingFileCompliance).equals(ComparisonSettings.Threshold)) {
            thresholds.put(StatusCategory.FileCompliance, this.fileComplianceIndex);
        }
        
        if(ComparisonSettings.valueOf(settingProjectCompliance).equals(ComparisonSettings.Threshold)) {
            thresholds.put(StatusCategory.ProjectCompliance, this.projectComplianceIndex);
        }
        
        if(ComparisonSettings.valueOf(settingMaxMessages).equals(ComparisonSettings.Threshold)) {
            thresholds.put(StatusCategory.Messages, this.totalMax);
        }        
    }
    
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new PRQAProjectAction(project);
    }
    
    
    /*
     *Small utility to handle illegal values. Defaults to null if string is unparsable. 
     * 
     */
    private static Integer parseIntegerNullDefault(String value) {
        try {
            
            if(value == null || value.equals("")) {
               return null;
            }
            
            Integer parsed = Integer.parseInt(value);
            return parsed;
        
        } catch (NumberFormatException nex) {
            return null;
        }
    }
    
    private static Double parseDoubleNullDefault(String value) {
        try 
        {
            if(value == null || value.equals("")) {
               return null;
            }
            Double parsed = Double.parseDouble(value);
            return parsed; 
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
    
    private void copyReportToArtifactsDir(PRQAReport report, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        FilePath[] files = build.getWorkspace().list("**/"+report.getNamingTemplate());
        if(files.length >= 1) {
            out.println("Found report. Attempting to copy "+report.getNamingTemplate()+" to artifacts directory: "+build.getArtifactsDir().getPath());
            String artifactDir = build.getArtifactsDir().getPath();

            FilePath targetDir = new FilePath(new File(artifactDir+"/"+report.getNamingTemplate()));
            out.println("Attempting to copy report to following target: "+targetDir.getName());

            build.getWorkspace().list("**/"+report.getNamingTemplate())[0].copyTo(targetDir);
            out.println("Succesfully copied report");
        }
    }
    
    public List<PRQAGraph> getSupportedGraphs(QARReportType type) {
        ArrayList<PRQAGraph> graphs = new ArrayList<PRQAGraph>();
        for(PRQAGraph g : graphTypes) {
            if(g.getType().equals(type)) {
                graphs.add(g);
            }
        }
        return graphs;
    }
    
    public PRQAGraph getGraph(String simpleClassName) {
        for(PRQAGraph p : getSupportedGraphs(reportType)) {
            if(p.getClass().getSimpleName().equals(simpleClassName)) {
                return p;
            }
        }            
        return null;
    }
    /**
     * Use this method to get the threshold values associated with the current build.
     * @param cat
     * @return the threshold for any given category.
     */
    public Number getThreshold(StatusCategory cat) {
        Number num = null;
        if(thresholds.containsKey(cat)) {
            num = thresholds.get(cat);
        }
        return num;
    }
    
    public HashMap<StatusCategory, Number> getThresholds() {
        return this.thresholds;
    }
    
    public PRQAGraph getGraph(Class clazz, List<PRQAGraph> graphs) {
        for(PRQAGraph p : graphs) {
            if(p.getClass().equals(clazz)) {
                return p;
            }
        }            
        return null;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        out = listener.getLogger();
        PRQAReading status = null;
        
        out.println(Config.getPluginVersion());
        out.println("");
        
        //Create a QAR command line instance. Sets the selected type of report. Used later when we construct the command.        
        QAR qar = new QAR(PRQA.create(product), projectFile, reportType);
        
        out.println("This job will create a report with the following selected parameters:");
        out.println(qar);

        Future<? extends PRQAReading> task = null;
        PRQAReport<?> report = null;

        try {     
            report = PRQAReport.create(reportType, qar);            
            switch(reportType) {
                case Compliance:                  
                    task = build.getWorkspace().actAsync(new PRQARemoteComplianceReport(report, listener, false));
                    break;
                case Quality:
                    task = build.getWorkspace().actAsync(new PRQARemoteQualityReport(report, listener, false));
                    break;
                case CodeReview:
                    task = build.getWorkspace().actAsync(new PRQARemoteCodeReviewReport(report, listener, false));
                    break;
                case Suppression:
                    task = build.getWorkspace().actAsync(new PRQARemoteSuppressionReport(report, listener, false));
                    break;
        }
            
        try {
            status = task.get();
            copyReportToArtifactsDir(report, build);
        } catch (ExecutionException ex) {
            out.print("Caught exception - Abnormal execution");
            throw new PrqaException.PrqaCommandLineException(qar, ex);
        } catch (Exception ex) {
            out.print("Caught exception - Abnormal execution");
            throw new PrqaException.PrqaCommandLineException(qar, ex);
        }
            
        } catch (IOException ex) {
            out.println("Caught IOExcetion with cause: "+ex.getCause().getMessage());
            out.println(ex.getCause().toString());            
        } catch (PrqaException ex) {
            out.println(ex);
        }
        
        if(status == null) {
            out.println("Failed getting results");
            return false;
        }
        
        status.setThresholds(thresholds);
       
        boolean res = true;
        Tuple<PRQAReading,AbstractBuild<?,?>> previousResult = getPreviousReading(build, Result.SUCCESS);
        
        if(previousResult != null) {
            out.println(String.format("Previous result (build number %s)",previousResult.getSecond().number));
            out.println(previousResult.getFirst());
        } else {
            out.println("No previous succesful builds");
        }
        
        PRQAReading lar = previousResult != null ? previousResult.getFirst() : null;
        
        ComparisonSettings fileCompliance = ComparisonSettings.valueOf(settingFileCompliance);
        ComparisonSettings projCompliance = ComparisonSettings.valueOf(settingProjectCompliance);
        ComparisonSettings maxMsg = ComparisonSettings.valueOf(settingMaxMessages);

        if(reportType.equals(QARReportType.Compliance)) {

            try {
                PRQAStatus.PRQAComparisonMatrix max_msg = status.createComparison(maxMsg, StatusCategory.Messages, lar);                
                PRQAStatus.PRQAComparisonMatrix proj_comp = status.createComparison(projCompliance, StatusCategory.ProjectCompliance, lar);
                PRQAStatus.PRQAComparisonMatrix file_comp = status.createComparison(fileCompliance, StatusCategory.FileCompliance, lar);

                if(!max_msg.compareIsEqualOrLower(totalMax)) {
                    status.addNotification(String.format("Max messages requirement not met, was %s and the requirement is %s",status.getReadout(StatusCategory.Messages),max_msg.getCompareValue()));
                    res = false;
                }

                if(!proj_comp.compareIsEqualOrHigher(projectComplianceIndex)) {
                    status.addNotification(String.format("Project Compliance Index not met, was %s and the required index is %s ",status.getReadout(StatusCategory.ProjectCompliance),file_comp.getCompareValue()));
                    res = false;
                }

                if(!file_comp.compareIsEqualOrHigher(fileComplianceIndex)) {
                    status.addNotification(String.format("File Compliance Index not met, was %s and the required index is %s ",status.getReadout(StatusCategory.FileCompliance),file_comp.getCompareValue()));
                    res = false;
                }

            } catch (PrqaException.PrqaReadingException ex) {
                out.println(ex);
            }
            out.println("Scanned the following values:");        
            out.println(status);   

        } else if(reportType.equals(QARReportType.Quality)) {
            out.println(status);
        } else if(reportType.equals(QARReportType.CodeReview)) {
            out.println(status);
        } else if (reportType.equals(QARReportType.Suppression)) {
            out.println(status);
        }   
              
        PRQABuildAction action = new PRQABuildAction(build);
        action.setResult(status);
        action.setPublisher(this); 
        if(!res)
            build.setResult(Result.UNSTABLE);        
        build.getActions().add(action);        
        return true; 
    }
    
    /**
     * Fetches the most 'previous' result. The current build is baseline. So any prior build to the passed current build is considered.
     * @param build
     * @param expectedResult
     * @return 
     */
    private Tuple<PRQAReading,AbstractBuild<?,?>> getPreviousReading(AbstractBuild<?,?> currentBuild, Result expectedResult) {
        Tuple<PRQAReading,AbstractBuild<?,?>> result = null;
        AbstractBuild<?,?> iterate = currentBuild;
        do {
            iterate = iterate.getPreviousNotFailedBuild();
            if(iterate != null && iterate.getAction(PRQABuildAction.class) != null && iterate.getResult().equals(expectedResult)) {
                result = new Tuple<PRQAReading, AbstractBuild<?, ?>>();
                result.setSecond(iterate);
                result.setFirst(iterate.getAction(PRQABuildAction.class).getResult());
                return result;
            }         
        } while(iterate != null);      
        return result;
    }
    
    @Exported
    public Integer getTotalMax() {
        return this.totalMax;
    }

    @Exported
    public void setTotalMax(Integer totalMax) {
        this.totalMax = totalMax;
    }
    
    @Exported
    public void setGraphTypes(List<PRQAGraph> graphTypes) {
        this.graphTypes = graphTypes;
    }
    
    @Exported
    public List<PRQAGraph> getGraphTypes() {
        return graphTypes;
    }
    
    @Exported
    public String getProduct() {
        return product;
    }

    @Exported
    public void setProduct(String product) {
        this.product = product;
    }

    @Exported
    public QARReportType getReportType() {
        return reportType;
    }

    @Exported
    public void setReportType(QARReportType reportType) {
        this.reportType = reportType;
    }

    @Exported
    public Boolean getTotalBetter() {
        return totalBetter;
    }

    @Exported
    public void setTotalBetter(Boolean totalBetter) {
        this.totalBetter = totalBetter;
    }
    
    @Exported 
    public void setFileComplianceIndex(Double fileCompliance) {
        this.fileComplianceIndex = fileCompliance;
    }
    
    @Exported
    public Double getFileComplianceIndex(){
        return this.fileComplianceIndex;        
    }
    
    @Exported
    public Double getProjectComplianceIndex() {
        return this.projectComplianceIndex;
    }
    
    @Exported 
    public void setProjectComplianceIndex(Double index) {
        this.projectComplianceIndex = index;
    }
      
    @Exported 
    public void setSettingFileCompliance(String settingFileCompliance) {
        this.settingFileCompliance = settingFileCompliance;
    }
    
    @Exported 
    public String getSettingFileCompliance() {
        return this.settingFileCompliance;
    }
    
    @Exported 
    public void setSettingProjectCompliance(String settingProjectCompliance) {
        this.settingProjectCompliance = settingProjectCompliance;
    }
    
    @Exported 
    public String getSettingProjectCompliance() {
        return this.settingProjectCompliance;
    }
    
    @Exported 
    public String getSettingMaxMessages() {
        return this.settingMaxMessages;
    }
    
    @Exported 
    public void setSettingMaxMessages(String settingMaxMessages) {
        this.settingMaxMessages = settingMaxMessages;
    }
    
    @Exported
    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }
    
    @Exported
    public String getProjectFile() {
        return this.projectFile;
    }
    
    /**
     * This class is used by Jenkins to define the plugin.
     * 
     * @author jes
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        
        public FormValidation doCheckFileComplianceIndex(@QueryParameter String value) {
            try {
                Double parsedValue = Double.parseDouble(value);
                if(parsedValue < 0)
                    return FormValidation.error("Decimal must be non-negative");
            } catch (NumberFormatException ex) {
                return FormValidation.error("Value is not a valid decimal number, use . to seperate decimals");
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckProjectComplianceIndex(@QueryParameter String value) {
            try {
                Double parsedValue = Double.parseDouble(value);
                if(parsedValue < 0)
                    return FormValidation.error("Decimal must be non-negative");
            } catch (NumberFormatException ex) {
                return FormValidation.error("Value is not a valid decimal number, use . to seperate decimals");
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckTotalMax(@QueryParameter String value) {
            try {
                Integer parsedValue = Integer.parseInt(value);
                if(parsedValue < 0)
                    return FormValidation.error("Number of messages must be zero or greater");
            } catch (NumberFormatException ex) {
                return FormValidation.error("Not a valid number, use a number without decimals");
            }
            return FormValidation.ok();
        }
        
        @Override
        public String getDisplayName() {
            return "Programming Research Report";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }        
            
        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {           
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);
            
            if(instance.getGraphTypes() == null || instance.getGraphTypes().isEmpty()) {
                ArrayList<PRQAGraph> list = new ArrayList<PRQAGraph>();
                
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
                list.add(new LinesOfCodeGraph());
                list.add(new MsgSupressionGraph());
                list.add(new NumberOfSourceFilesGraph());
                list.add(new NumberOfFilesGraph(QARReportType.Quality));
                list.add(new NumberOfFilesGraph(QARReportType.Suppression));
                list.add(new PercentSuppressionGraph());
                
                list.add(new NumberOfFileMetricsGraph());
                list.add(new NumberOfFunctionGraph());
                list.add(new NumberOfFunctionMetricsGraph());
                
                //Added for Cpp reports:
                list.add(new NumberOfClassMetricsGraph());
                list.add(new NumberOfClassesGraph());
                                
                instance.setGraphTypes(list);
            }
            
            save();
            return instance;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            save();
            return super.configure(req, json);
        }
               
        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }

        public QARReportType[] getReports() {
            QARReportType[] types = new QARReportType[1];
            types[0] = QARReportType.Compliance;
            return types;
        }

        public List<String> getProducts() {
            List<String> s = new ArrayList<String>();
            for (AnalysisTools a : AnalysisTools.values()) {
                s.add(a.toString());
            }
            return s;
        }
        
        public List<String> getComparisonSettings() {
            List<String> settings = new ArrayList<String>();
            for (ComparisonSettings setting : ComparisonSettings.values()) {
                settings.add(setting.toString());
            }
            return settings;
        }       
    }
}
