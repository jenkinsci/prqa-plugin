/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import net.praqma.jenkins.plugin.prqa.Config;
import net.praqma.jenkins.plugin.prqa.PRQARemoteComplianceReport;
import net.praqma.prqa.PRQAContext.AnalysisTools;
import net.praqma.prqa.PRQAContext.ComparisonSettings;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAStatus.ComplianceCategory;
import net.praqma.prqa.products.PRQACommandBuilder;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQACodeReviewReport;
import net.praqma.prqa.reports.PRQAComplianceReport;
import net.praqma.prqa.reports.PRQAQualityReport;
import net.praqma.prqa.reports.PRQASupressionReport;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {
    private PrintStream out;
        
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
        this.reportType = QARReportType.valueOf(reportType);
        this.product = product;
        this.totalBetter = totalBetter;
        this.totalMax = parseIntegerNullDefault(totalMax);
        this.fileComplianceIndex = parseDoubleNullDefault(fileComplianceIndex);
        this.projectComplianceIndex = parseDoubleNullDefault(projectComplianceIndex);
        this.settingProjectCompliance = settingProjectCompliance;
        this.settingMaxMessages = settingMaxMessages;
        this.settingFileCompliance = settingFileCompliance;
        this.projectFile = projectFile;
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
        return BuildStepMonitor.NONE;
    }
    
    private void copyReportToArtifactsDir(String reportFile, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        FilePath[] files = build.getWorkspace().list("**/"+reportFile);
        if(files.length >= 1) {
            out.println("Found report. Attempting to copy "+reportFile+" to artifacts directory: "+build.getArtifactsDir().getPath());
            String artifactDir = build.getArtifactsDir().getPath();

            FilePath targetDir = new FilePath(new File(artifactDir+"/"+reportFile));
            out.println("Attempting to copy report to following target: "+targetDir.getName());

            build.getWorkspace().list("**/"+reportFile)[0].copyTo(targetDir);
            out.println("Succesfully copied report");
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        out = listener.getLogger();
        PRQAReading status = null;
        //Create a QAR command line instance.
        QAR qar = new QAR();
        qar.setType(reportType);
        
        qar.getBuilder().prependArgument(PRQACommandBuilder.getProduct(product.toString()));
        qar.getBuilder().appendArgument(PRQACommandBuilder.getProjectFile(projectFile));
                
        try {
            switch(reportType) {
                case Compliance:
                    PRQAComplianceReport testReport = new PRQAComplianceReport(qar);                  
                    status = build.getWorkspace().act(new PRQARemoteComplianceReport(testReport,listener,false));
                    copyReportToArtifactsDir(PRQAComplianceReport.getNamingTemplate(), build);
                    break;
                case Quality:
                    copyReportToArtifactsDir(PRQAQualityReport.getNamingTemplate(), build);
                    break;
                case CodeReview:
                    copyReportToArtifactsDir(PRQACodeReviewReport.getNamingTemplate(), build);
                    break;
                case Supression:
                    copyReportToArtifactsDir(PRQASupressionReport.getNamingTemplate(), build);
                    break;
            }
        } catch (IOException ex) {
            out.println("Caught IOExcetion with cause: ");
            out.println(ex.getCause().toString());
        }
        
        if(status == null) {
            out.println("Failed getting results");
            return false;
        }
                 
        boolean res = true;
        
        if(reportType.equals(QARReportType.Compliance)) {
        
            ComparisonSettings fileCompliance = ComparisonSettings.valueOf(settingFileCompliance);
            ComparisonSettings projCompliance = ComparisonSettings.valueOf(settingProjectCompliance);
            ComparisonSettings maxMsg = ComparisonSettings.valueOf(settingMaxMessages);

            //Check to see if any of the options include previous build comparisons. If it does instantiate last build
            if(fileCompliance.equals(ComparisonSettings.Improvement) || projCompliance.equals(ComparisonSettings.Improvement) || maxMsg.equals(ComparisonSettings.Improvement)) {
                PRQAReading lastAction = build.getPreviousBuild().getAction(PRQABuildAction.class).getResult();

                if(fileCompliance.equals(ComparisonSettings.Improvement)) {
                    if(lastAction.getReadout(ComplianceCategory.FileCompliance).doubleValue() > status.getReadout(ComplianceCategory.FileCompliance).doubleValue()) {
                        status.addNotification(String.format("File Compliance Index not met, was %s and the required index is %s ",status.getReadout(ComplianceCategory.FileCompliance),lastAction.getReadout(ComplianceCategory.FileCompliance)));
                        res = false;
                    }
                }

                if(projCompliance.equals(ComparisonSettings.Improvement)) {
                    if(lastAction.getReadout(ComplianceCategory.ProjectCompliance).doubleValue() > status.getReadout(ComplianceCategory.ProjectCompliance).doubleValue()) {
                        status.addNotification(String.format("Project Compliance Index not met, was %s and the required index is %s ",status.getReadout(ComplianceCategory.ProjectCompliance),lastAction.getReadout(ComplianceCategory.ProjectCompliance)));
                        res = false;
                    }
                }

                if(maxMsg.equals(ComparisonSettings.Improvement)) {
                    if(lastAction.getReadout(ComplianceCategory.Messages).intValue() < status.getReadout(ComplianceCategory.Messages).intValue()) {
                        status.addNotification(String.format("Number of messages exceeds the requiremnt, is %s, requirement is %s", status.getReadout(ComplianceCategory.Messages),lastAction.getReadout(ComplianceCategory.Messages)));
                        res = false;
                    }
                }

            }

            if(fileCompliance.equals(ComparisonSettings.Threshold) && status.getReadout(ComplianceCategory.FileCompliance).doubleValue() < fileComplianceIndex ) {
                status.addNotification(String.format("File Compliance Index not met, was %s and the required index is %s ",status.getReadout(ComplianceCategory.FileCompliance),fileComplianceIndex));
                res = false;
            }

            if(projCompliance.equals(ComparisonSettings.Threshold) && status.getReadout(ComplianceCategory.ProjectCompliance).doubleValue() < projectComplianceIndex) {
                status.addNotification(String.format("Project Compliance Index not met, was %s and the required index is %s ",status.getReadout(ComplianceCategory.ProjectCompliance),projectComplianceIndex));
                res = false;
            }

            if(maxMsg.equals(ComparisonSettings.Threshold) && status.getReadout(ComplianceCategory.Messages).intValue() > totalMax) {
                status.addNotification(String.format("Number of messages exceeds the requiremnt, is %s, requirement is %s", status.getReadout(ComplianceCategory.Messages),totalMax));
                res = false;
            }

            out.println("Scanned the following values:");        
            out.println(status);

            status.disable(ComplianceCategory.FileCompliance);


        }
        
        PRQABuildAction action = new PRQABuildAction(build);
        action.setResult(status);
        action.setPublisher(this);  
        
        
        if(!res)
            build.setResult(Result.UNSTABLE);        
        build.getActions().add(action);        
        return true;      
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
        public PRQANotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {           
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);
            return instance;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return true;
        }
               
        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }

        public List<String> getReports() {
            return Config.getReports();
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
