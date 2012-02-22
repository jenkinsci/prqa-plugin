/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.plugin.prqa.PrqaException;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.praqma.jenkins.plugin.prqa.Config;
import net.praqma.jenkins.plugin.prqa.parser.HtmlParser;
import net.praqma.prqa.PRQAContext.AnalyseTypes;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAContext.ComparisonSettings;
import net.praqma.prqa.products.QAC;
import net.praqma.prqa.products.QACpp;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {    
    private PrintStream out;
    private Status status;
    private Boolean totalBetter;
    private Integer totalMax;
    private String product;
    private QARReportType reportType;
    private String command;
    private String prqaHome;
    
    //Strings indicating the selected options.
    private String settingFileCompliance;
    private String settingMaxMessages;
    private String settingProjectCompliance;
    
    private Double fileComplianceIndex;
    private Double projectComplianceIndex;

    @DataBoundConstructor
    public PRQANotifier(String command, String reportType, String product,
            boolean totalBetter, String totalMax, String fileComplianceIndex, String projectComplianceIndex, String settingMaxMessages, String settingFileCompliance, String settingProjectCompliance) {
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        boolean result = false;

        out = listener.getLogger();
        //This is where we exectuate the report generation. 
        executed();
        
        //Next up. Let us parse the output.

        status = new Status();
        Pattern totalMessagesPattern = Pattern.compile("<td align=\"left\">Total Number of Messages</td>\n<td align=\"right\">(\\d*)</td>");
        Pattern fileCompliancePattern = Pattern.compile("<td align=\"left\">File Compliance Index</td>\n<td align=\"right\">(\\S*)%</td>");
        Pattern projectCompliancePattern = Pattern.compile("<td align=\"left\">Project Compliance Index</td>\n<td align=\"right\">(\\S*)%</td>");
                      

        try {
            out.println("Compliance report path: " + Config.COMPLIANCE_REPORT_PATH);
            if(!HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, totalMessagesPattern).isEmpty()) {
                Integer tmp = Integer.parseInt(HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, totalMessagesPattern).get(0)); 
                status.setMessages(tmp);
            }
            
            if(!HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, fileCompliancePattern).isEmpty()) {
                Double tmp = Double.parseDouble(HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, fileCompliancePattern).get(0));
                status.setFileCompliance(tmp);
            }
            
            if(!HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, projectCompliancePattern).isEmpty()) {
                Double tmp = Double.parseDouble(HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, projectCompliancePattern).get(0));
                status.setProjectCompliance(tmp);
            }
            
        } catch (PrqaException ex) {
            Logger.getLogger(PRQANotifier.class.getName()).log(Level.SEVERE, null, ex);
        }

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
                    res = false;
                }
            }

            if(projCompliance.equals(ComparisonSettings.Improvement)) {
                if(publisher.getStatus().getFileCompliance() > status.getProjectCompliance()) {
                    res = false;
                }
            }

            if(maxMsg.equals(ComparisonSettings.Improvement)) {
                if(publisher.getStatus().getMessages() < status.getMessages()) {
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
        
        final PRQABuildAction action = new PRQABuildAction(build);
        action.setPublisher(this);
        build.getActions().add(action);
        
        return res;
    }

    private void executed() {

        switch (AnalyseTypes.valueOf(product)) {
            case QAC:
                new QAC().execute(command);
                break;
            case QACpp:
                new QACpp().execute(command);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Exported
    public String getPrqaHome() {
        return prqaHome;
    }

    @Exported
    public void setPrqaHome(String prqaHome) {
        this.prqaHome = prqaHome;
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

    public Status getStatus() {
        return this.status;
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

    @Override
    public String toString() {
        return status.toString();
    }
    
    
    
    /**
     * This class is used by Jenkins to define the plugin.
     * 
     * @author jes
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String prqaHome;
        
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

        String getPrqaHome() {
            return prqaHome;
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
            for (AnalyseTypes a : AnalyseTypes.values()) {
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
