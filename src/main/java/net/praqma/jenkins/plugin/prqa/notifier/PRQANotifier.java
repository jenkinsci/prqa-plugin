/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import net.praqma.jenkins.plugin.prqa.Config;
import net.praqma.jenkins.plugin.prqa.PRQARemoteAnalysis;
import net.praqma.jenkins.plugin.prqa.PRQARemoteReporting;
import net.praqma.prqa.PRQAComplianceStatus;
import net.praqma.prqa.PRQAComplianceStatusCollection;
import net.praqma.prqa.PRQAContext.AnalysisTools;
import net.praqma.prqa.PRQAContext.ComparisonSettings;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.products.QAC;
import net.praqma.prqa.products.QACpp;
import net.praqma.prqa.products.QAR;
import net.sf.json.JSONObject;
import org.apache.commons.lang.NotImplementedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {    
    private PrintStream out;
    private PRQAComplianceStatus status = new PRQAComplianceStatus();
    private Boolean totalBetter;
    private Integer totalMax;
    private String product;
    private QARReportType reportType;
    
    private String command;
    private String qarCommand;
    
    private String qacHome;
    private String qacppHome;
    private String qarHome;
    
    
    //Strings indicating the selected options.
    private String settingFileCompliance;
    private String settingMaxMessages;
    private String settingProjectCompliance;
    
    private Double fileComplianceIndex;
    private Double projectComplianceIndex;
    
    private String projectFile;

    @DataBoundConstructor
    public PRQANotifier(String command, String reportType, String product,
            boolean totalBetter, String totalMax, String fileComplianceIndex, String projectComplianceIndex, String settingMaxMessages, String settingFileCompliance, String settingProjectCompliance, String qacppHome, String qacHome, String qarHome, String qarCommand, String projectFile) {
        this.reportType = QARReportType.valueOf(reportType);
        this.product = product;
        this.totalBetter = totalBetter;
        this.totalMax = parseIntegerNullDefault(totalMax);
        this.command = command;       
        this.fileComplianceIndex = parseDoubleNullDefault(fileComplianceIndex);
        this.projectComplianceIndex = parseDoubleNullDefault(projectComplianceIndex);
        this.settingProjectCompliance = settingProjectCompliance;
        this.settingMaxMessages = settingMaxMessages;
        this.settingFileCompliance = settingFileCompliance;
        this.qacppHome = qacppHome;
        this.qacHome = qacHome;
        this.qarHome = qarHome;
        this.qarCommand = qarCommand;
        
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
        
        } catch (NullPointerException nex) {
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
        //TODO copied from CCUCM NOTIFIER ask chw why this is like this????
        return BuildStepMonitor.NONE;
    }
    

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        out = listener.getLogger();
        //We need to do this on
        //build.getWorkspace().act(new PRQARemoteAnalysis(projectFile, qarHome, listener));                
        //First. Run the analysis tool. Depending on which product is selected. This should produce a project file.
        
        switch (AnalysisTools.valueOf(product)) {
            case QAC:
                //new QAC(qacHome).execute(command);
                QAC qac = new QAC(qacHome, command);
                build.getWorkspace().act(new PRQARemoteAnalysis(qac, listener, launcher));
                break;
            case QACpp:
                //new QACpp(qacppHome).execute(command);
                QACpp qacpp = new QACpp(qacppHome,command);
                build.getWorkspace().act(new PRQARemoteAnalysis(qacpp, listener, launcher));
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        //This is where we exectuate the report generation. Currently only the compliance report is available.
        String qarCommandSubstituted = qarCommand;
        
        qarCommandSubstituted = qarCommandSubstituted.replaceFirst("\\{%product}", product.toString());
        qarCommandSubstituted = qarCommandSubstituted.replaceFirst("\\{%reportType}", reportType.toString());       
        qarCommandSubstituted = qarCommandSubstituted.replaceFirst("\\{%projectFile}", projectFile.toString());      
        qarCommandSubstituted = qarCommandSubstituted.replaceFirst("\\{%outpath}","\""+Matcher.quoteReplacement(build.getArtifactsDir().getPath())+"\"");
        out.println(qarCommandSubstituted);
        
        
        //Create a QAR command line instance.
        QAR qar = new QAR(qarHome); 
        try {
            switch(reportType) {
                case Compliance:

                    status = build.getWorkspace().act(new PRQARemoteReporting(qar, listener));
                    //PRQAComplianceReport rep = new PRQAComplianceReport(qar);
                    //out.println(rep.getFullReportPath());
                    //TODO: Remove this after report generation 
                    //FileUtils.copyFile(new File(Config.COMPLIANCE_REPORT_PATH), new File(rep.getFullReportPath()));
                    //status = rep.completeTask();
                    break;
                case Quality:
                    throw new NotImplementedException("Not implemented yet");            
                case CodeReview:
                    throw new NotImplementedException("Not implemented yet");
                case Supression:
                    throw new NotImplementedException("Not implemented yet");
            }
        } catch (IOException ex) {
            out.println(ex.toString());
        } 
/*        
        catch (PrqaException.PrqaParserException pex) {
            out.println(pex.getMessage());
            out.println(pex.getRootCause().getMessage());
            return false;
        } catch (PrqaException.PrqaCommandLineException ex) {
            out.println(ex.toString());
            out.println(ex.getRootCause().toString());
            return false;
        } catch (PrqaException prqaex) {
            out.println(prqaex.getMessage());
            return false;
        }
*/       
        boolean res = true;
        
        ComparisonSettings fileCompliance = ComparisonSettings.valueOf(settingFileCompliance);
        ComparisonSettings projCompliance = ComparisonSettings.valueOf(settingProjectCompliance);
        ComparisonSettings maxMsg = ComparisonSettings.valueOf(settingMaxMessages);
        
        
        //Check to see if any of the options include previous build comparisons. If it does instantiate last build
        if(fileCompliance.equals(ComparisonSettings.Improvement) || projCompliance.equals(ComparisonSettings.Improvement) || maxMsg.equals(ComparisonSettings.Improvement)) {
            
            AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
            DescribableList<Publisher, Descriptor<Publisher>> publishers = prevBuild.getProject().getPublishersList();
            PRQANotifier publisher = (PRQANotifier) publishers.get(PRQANotifier.class);
            
            if (publisher == null) {
                throw new IOException();
            }
                       
            if(fileCompliance.equals(ComparisonSettings.Improvement)) {

                if(publisher.getStatus().getFileCompliance() > status.getFileCompliance()) {
                    status.addNotication(String.format("File Compliance Index not met, was %s and the required index is %s ",status.getFileCompliance(),publisher.getStatus().getFileCompliance()));
                    res = false;
                }
            }

            if(projCompliance.equals(ComparisonSettings.Improvement)) {
                if(publisher.getStatus().getProjectCompliance() > status.getProjectCompliance()) {
                    status.addNotication(String.format("Project Compliance Index not met, was %s and the required index is %s ",status.getProjectCompliance(),publisher.getStatus().getProjectCompliance()));
                    res = false;
                }
            }

            if(maxMsg.equals(ComparisonSettings.Improvement)) {
                if(publisher.getStatus().getMessages() < status.getMessages()) {
                    status.addNotication(String.format("Number of messages exceeds the requiremnt, is %s, requirement is %s", status.getMessages(),publisher.getStatus().getMessages()));
                    res = false;
                }
            }
                             
        }
       
        if(fileCompliance.equals(ComparisonSettings.Threshold) && status.getFileCompliance() < fileComplianceIndex ) {
            status.addNotication(String.format("File Compliance Index not met, was %s and the required index is %s ",status.getFileCompliance(),fileComplianceIndex));
            res = false;
        }
        
        if(projCompliance.equals(ComparisonSettings.Threshold) && status.getProjectCompliance() < projectComplianceIndex) {
            status.addNotication(String.format("Project Compliance Index not met, was %s and the required index is %s ",status.getProjectCompliance(),projectComplianceIndex));
            res = false;
        }
        
        if(maxMsg.equals(ComparisonSettings.Threshold) && status.getMessages() > totalMax) {
            status.addNotication(String.format("Number of messages exceeds the requiremnt, is %s, requirement is %s", status.getMessages(),totalMax));
            res = false;
        }
        
        out.println("Scanned the following values:");        
        out.println(status);
        
        status.disable(PRQAComplianceStatusCollection.ComplianceCategory.FileCompliance);
   
        final PRQABuildAction action = new PRQABuildAction(build);
        action.setPublisher(this);
        
        if(!res)
            build.setResult(Result.UNSTABLE);        
        build.getActions().add(action);            
        return true;      
    }

    @Exported
    public String getQarHome() {
        return qarHome;
    }

    @Exported
    public void setQarHome(String qarHome) {
        this.qarHome = qarHome;
    }
    
    @Exported
    public String getQacHome() {
        return qacHome;
    }

    @Exported
    public void setQacHome(String qacHome) {
        this.qacHome = qacHome;
    }
    
    @Exported
    public String getQacppHome() {
        return qacppHome;
    }

    @Exported
    public void setQacppHome(String qacppHome) {
        this.qacppHome = qacppHome;
    }

    @Exported
    public String getCommand() {
        return command;
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
    public void setCommand(String Command) {
        this.command = Command;
    }
    
    @Exported
    public void setQarCommand(String qarCommand) {
        this.qarCommand = qarCommand;
    }
    
    @Exported
    public String getQarCommand() {
        return this.qarCommand;
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

    public PRQAComplianceStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(PRQAComplianceStatus status) {
        this.status = status;
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

    @Override
    public String toString() {
        return status != null ? status.toString() : "No results have been generated";
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
        
        
        /*
        public FormValidation doCheckQacHome(@QueryParameter String value) {
            return new File(value).exists() ? FormValidation.ok() : FormValidation.error(String.format("No executable or directory found with specified path: %s",value));
        }

        public FormValidation doCheckQacppHome(@QueryParameter String value) {         
            return new File(value).exists() ? FormValidation.ok() : FormValidation.error(String.format("No executable or directory found with specified path: %s",value));
        }
        
        public FormValidation doCheckQarHome(@QueryParameter String value) {
            return new File(value).exists() ? FormValidation.ok() : FormValidation.error(String.format("No executable or directory found with specified path: %s",value));
        }
        
        public FormValidation doCheckProjectFile(@QueryParameter String value) {
            return new File(value).exists() ? FormValidation.ok() : FormValidation.error(String.format("No file found on the given path: %s", value));              
        }
        */
        
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
            super.configure(req, formData);
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);
            return instance;
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
