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
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.plugin.prqa.Config;
import net.praqma.jenkins.plugin.prqa.PRQARemoteReport;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.graphs.*;
import net.praqma.jenkins.plugin.prqa.setup.PRQAToolSuite;
import net.praqma.jenkins.plugin.prqa.setup.QACToolSuite;
import net.praqma.jenkins.plugin.prqa.setup.QACppToolSuite;
import net.praqma.prga.excetions.PrqaException;
import net.praqma.prqa.CodeUploadSetting;
import net.praqma.prqa.PRQA;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAContext.ComparisonSettings;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAReportSettings;
import net.praqma.prqa.PRQAUploadSettings;
import net.praqma.prqa.QAVerifyServerSettings;
import net.praqma.prqa.products.QAR;
import net.praqma.prqa.reports.PRQAReport;
import net.praqma.prqa.reports.PRQAReport2;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.StatusCategory;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {
    /*
    public final String qacTool;
    public final String qacppTool;
    */
    private PrintStream out;
    private List<PRQAGraph> graphTypes;
    private HashMap<StatusCategory,Number> thresholds;
    private EnumSet<QARReportType> chosenReportTypes;
    public final int threshholdlevel;
 
    private Boolean totalBetter;
    private Integer totalMax;
    public final String product;
    
    private String chosenServer;
 
    private String settingFileCompliance;
    private String settingMaxMessages;
    private String settingProjectCompliance;
    
    private Double fileComplianceIndex;
    private Double projectComplianceIndex;
    
    private String projectFile;
    private String vcsConfigXml;
    private boolean performCrossModuleAnalysis;
    private boolean publishToQAV;
    private boolean singleSnapshotMode;
    
    //RQ-1
    private boolean enableDependencyMode;
    private boolean enableDataFlowAnalysis;
    
    //RQ-3
    private boolean generateReports;
    
    //RQ-7
    private CodeUploadSetting codeUploadSetting = CodeUploadSetting.None;
    
    //RQ-9
    private String sourceOrigin;
    
    private String qaVerifyProjectName;

    @DataBoundConstructor
    public PRQANotifier(final String product, boolean totalBetter, 
    String totalMax, String fileComplianceIndex, String projectComplianceIndex, 
    String settingMaxMessages, String settingFileCompliance, String settingProjectCompliance, 
    String projectFile, boolean performCrossModuleAnalysis, boolean publishToQAV, 
    String qaVerifyProjectName, String vcsConfigXml, boolean singleSnapshotMode,
            String snapshotName, String chosenServer, boolean enableDependencyMode, 
            String codeUploadSetting, String msgConfigFile, boolean generateReports, String sourceOrigin, EnumSet<QARReportType> chosenReportTypes,
            boolean enableDataFlowAnalysis, final int threshholdlevel) {
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
        this.publishToQAV = publishToQAV;
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
        this.vcsConfigXml = vcsConfigXml;
        this.singleSnapshotMode = singleSnapshotMode;
        this.qaVerifyProjectName = qaVerifyProjectName;
        this.chosenServer = chosenServer;
        this.enableDependencyMode = enableDependencyMode;
        this.codeUploadSetting = CodeUploadSetting.getByValue(codeUploadSetting);
        this.sourceOrigin = sourceOrigin;
        this.chosenReportTypes = chosenReportTypes;
        this.enableDataFlowAnalysis = enableDataFlowAnalysis;
        this.threshholdlevel = threshholdlevel;
        
        this.generateReports = generateReports;
 
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
   
    private void copyReourcesToArtifactsDir(String pattern, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        FilePath[] files = build.getWorkspace().list("**/"+pattern);
        if(files.length >= 1) {
            for(int i = 0; i<files.length; i++) { 
                String artifactDir = build.getArtifactsDir().getPath();
                FilePath targetDir = new FilePath(new File(artifactDir+"/"+files[i].getName()));

                files[i].copyTo(targetDir);
                out.println(Messages.PRQANotifier_SuccesFileCopy(files[i].getName()));
                
            }
        }
    }

    private void copyReportsToArtifactsDir(PRQAReportSettings settings, AbstractBuild<?, ?> build) throws IOException, InterruptedException {        
        for(PRQAContext.QARReportType type : settings.chosenReportTypes) {
            String pattern = "**/"+PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION);
            FilePath[] files = build.getWorkspace().list(pattern);
            if(files.length >= 1) {
                out.println(Messages.PRQANotifier_FoundReport(PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION)));                
                String artifactDir = build.getArtifactsDir().getPath();

                FilePath targetDir = new FilePath(new File(artifactDir+"/"+PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION)));
                out.println(Messages.PRQANotifier_CopyToTarget(targetDir.getName()));
                
                build.getWorkspace().list("**/"+PRQAReport.getNamingTemplate(type, PRQAReport.XHTML_REPORT_EXTENSION))[0].copyTo(targetDir);
                out.println(Messages.PRQANotifier_SuccesCopyReport());
            }
        }
    }
    
    public List<PRQAGraph> getSupportedGraphs() {
        ArrayList<PRQAGraph> graphs = new ArrayList<PRQAGraph>();
        for(PRQAGraph g : graphTypes) {
            if(g.getType().equals(QARReportType.Compliance)) {
                graphs.add(g);
            }
        }
        return graphs;
    }
    
    public PRQAGraph getGraph(String simpleClassName) {
        for(PRQAGraph p : getSupportedGraphs()) {
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
 
    /**
     * Pre-build for the plugin. We use this one to clean up old report files.
     * @param build
     * @param listener
     * @return 
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        try {
            List<FilePath> files = build.getWorkspace().list(new ReportFileFilter());
            int numberOfReportFiles = build.getWorkspace().list(new ReportFileFilter()).size();
            listener.getLogger().println(String.format( "Found %s report fragments, cleaning up", numberOfReportFiles));
            int deleteCounter = 0;
            for(FilePath f : files) {
                f.delete();
                deleteCounter++;
            }
            listener.getLogger().println( String.format("Succesfully deleted %s report fragments", deleteCounter) );
            
        } catch (IOException ex) {
            ex.printStackTrace(listener.getLogger());
            Logger.getLogger(PRQANotifier.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            ex.printStackTrace(listener.getLogger());
            Logger.getLogger(PRQANotifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return true;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        
        String productUsed = product;
        out = listener.getLogger();
        PRQAReading status = null;
        PRQAToolSuite suite = null;
        QACToolSuite qacSuite = QACToolSuite.getInstallationByName(product);
        
        if(qacSuite != null) {
            productUsed = qacSuite.tool;
            suite = qacSuite;
        }
        
        out.println(Config.getPluginVersion());
        QAR qar = new QAR(PRQA.create(productUsed), projectFile, QARReportType.Compliance);
        
        if(generateReports) {
            out.println(Messages.PRQANotifier_ReportGenerateText());
            out.println(qar);
        } else {
            out.println("No reports selected.");
        }
        
        QAVerifyServerConfiguration conf = PRQAGlobalConfig.get().getConfigurationByName(chosenServer);        
 
        PRQAApplicationSettings appSettings = null;
        if(suite != null) {
            if(suite instanceof QACToolSuite) {
                QACToolSuite cSuite = (QACToolSuite)suite;
                appSettings = new PRQAApplicationSettings(cSuite.qarHome, cSuite.qavHome, cSuite.qawHome);            
            } 
        }
        
        PRQAReportSettings settings = new PRQAReportSettings(chosenServer, projectFile,
                performCrossModuleAnalysis, publishToQAV, enableDependencyMode, 
                enableDataFlowAnalysis, chosenReportTypes, productUsed);
        
        PRQAUploadSettings uploadSettings = new PRQAUploadSettings(vcsConfigXml, singleSnapshotMode, codeUploadSetting, sourceOrigin, qaVerifyProjectName);
        
        
        QAVerifyServerSettings qavSettings = null;
        if(conf != null) {
            qavSettings = new QAVerifyServerSettings(conf.getHostName(), conf.getPortNumber(), conf.getProtocol(), conf.getPassword(), conf.getUserName());                    
        }        

        try {
            PRQAReport2 report2;
            if(suite != null) {
                out.println("Creating report WITH environment injected");
                
                HashMap<String,String> test = suite.createEnvironmentVariables(build.getWorkspace().getRemote());
                out.println("Created environment: "+test.size());
                report2 = new PRQAReport2(settings, qavSettings, uploadSettings, appSettings, suite.createEnvironmentVariables(build.getWorkspace().getRemote()));
            } else {
                out.println("Creating report WITHOUT environment injected");
                report2 = new PRQAReport2(settings, qavSettings, uploadSettings, appSettings, null);
            }
            listener.getLogger().println("Launching on linux???: "+launcher.isUnix());
            status = build.getWorkspace().act(new PRQARemoteReport(report2, listener, launcher.isUnix()));
        
        } catch (Exception ex) {
            out.println(Messages.PRQANotifier_FailedGettingResults());
            ex.printStackTrace(out);
            return false;
        } finally {
            try {
                if(generateReports) {
                    copyReportsToArtifactsDir(settings, build);
                }
                copyReourcesToArtifactsDir("*.log", build);
            } catch (Exception ex) {
                ex.printStackTrace(out);
            }
        }
        
        if(status == null && generateReports) {
            out.println(Messages.PRQANotifier_FailedGettingResults());
            return false;
        }
        
        Tuple<PRQAReading,AbstractBuild<?,?>> previousResult = getPreviousReading(build, Result.SUCCESS);
        
        if(status == null && !generateReports) {
            out.println(Messages.PRQANotifier_SkipOk());
            return true;
        }
        
        status.setThresholds(thresholds);
       
        boolean res = true;        
        
        if(previousResult != null) {
            out.println(String.format(Messages.PRQANotifier_PreviousResultBuildNumber(new Integer(previousResult.getSecond().number))));
            out.println(previousResult.getFirst());
        } else {
            out.println(Messages.PRQANotifier_NoPreviousResults());
        }
        
        PRQAReading lar = previousResult != null ? previousResult.getFirst() : null;
        
        //None, Threshold, Improvement;
        
        ComparisonSettings fileCompliance = ComparisonSettings.valueOf(settingFileCompliance);
        ComparisonSettings projCompliance = ComparisonSettings.valueOf(settingProjectCompliance);
        ComparisonSettings maxMsg = ComparisonSettings.valueOf(settingMaxMessages);
        
        
        //First compare file compliance
        try {
            Double currentFileCompliance = status.getReadout(StatusCategory.ProjectCompliance).doubleValue();
            if(fileCompliance == ComparisonSettings.Improvement) {
                if(lar != null) {
                    Double previous = lar.getReadout(StatusCategory.FileCompliance).doubleValue();
                    if(currentFileCompliance < previous) {
                        status.addNotification(Messages.PRQANotifier_ProjectComplianceIndexRequirementNotMet(currentFileCompliance, previous));
                        res = false;
                    }
                }

            } else if(fileCompliance == ComparisonSettings.Threshold) {
                if(currentFileCompliance < fileComplianceIndex) {
                    status.addNotification(Messages.PRQANotifier_FileComplianceRequirementNotMet(currentFileCompliance, fileComplianceIndex));
                    res = false;
                }
            }
            
            Double currentProjecCompliance = status.getReadout(StatusCategory.ProjectCompliance).doubleValue();
            if(projCompliance == ComparisonSettings.Threshold) {
                if(currentProjecCompliance < projectComplianceIndex) {
                    status.addNotification(Messages.PRQANotifier_ProjectComplianceIndexRequirementNotMet(currentProjecCompliance, projectComplianceIndex));
                    res = false;
                }

            } else if(projCompliance == ComparisonSettings.Improvement) {
                if(lar != null) {
                    Double previous = lar.getReadout(StatusCategory.ProjectCompliance).doubleValue();
                    if(currentProjecCompliance < previous) {
                        status.addNotification(Messages.PRQANotifier_ProjectComplianceIndexRequirementNotMet(currentProjecCompliance, previous));
                        res = false;
                    }
                }
            }
        } catch (PrqaException ex) {
            out.println(ex);
        }

        int current = ((PRQAComplianceStatus)status).getMessageCount(threshholdlevel);
        if(maxMsg == ComparisonSettings.Improvement) {
            if(lar != null) {
                int previous = ((PRQAComplianceStatus)lar).getMessageCount(threshholdlevel);
                if(current > previous) {
                    status.addNotification(Messages.PRQANotifier_MaxMessagesRequirementNotMet(current, previous));
                    res = false;
                }
            }

        } else if(maxMsg == ComparisonSettings.Threshold) {
            if(current > totalMax) {
                status.addNotification(Messages.PRQANotifier_MaxMessagesRequirementNotMet(current, totalMax));
                res = false;
            }
        }
        
        out.println(Messages.PRQANotifier_ScannedValues());        
        out.println(status);

        PRQABuildAction action = new PRQABuildAction(build);
        action.setResult(status);
        action.setPublisher(this); 
        if(!res) {
            build.setResult(Result.UNSTABLE);        
        }
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
    
    /*
    @Exported
    public String getProduct() {
        return product;
    }

    @Exported
    public void setProduct(String product) {
        this.product = product;
    }
    */

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
     * @return the performCrossModuleAnalysis
     */
    @Exported
    public boolean isPerformCrossModuleAnalysis() {
        return performCrossModuleAnalysis;
    }

    /**
     * @param performCrossModuleAnalysis the performCrossModuleAnalysis to set
     */
    @Exported
    public void setPerformCrossModuleAnalysis(boolean performCrossModuleAnalysis) {
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
    }

    /**
     * @return the publishToQAV
     */
    @Exported
    public boolean isPublishToQAV() {
        return publishToQAV;
    }
    
    @Exported
    public void setPublishToQAV(boolean publishToQAV) {
        this.publishToQAV = publishToQAV;
    }

    /**
     * @return the qaVerifyProjectName
     */
    public String getQaVerifyProjectName() {
        return qaVerifyProjectName;
    }

    /**
     * @param qaVerifyProjectName the qaVerifyProjectName to set
     */
    public void setQaVerifyProjectName(String qaVerifyProjectName) {
        this.qaVerifyProjectName = qaVerifyProjectName;
    }

    /**
     * @return the vcsConfigXml
     */
    public String getVcsConfigXml() {
        return vcsConfigXml;
    }

    /**
     * @param vcsConfigXml the vcsConfigXml to set
     */
    public void setVcsConfigXml(String vcsConfigXml) {
        this.vcsConfigXml = vcsConfigXml;
    }

    /**
     * @return the singleSnapshotMode
     */
    public boolean isSingleSnapshotMode() {
        return singleSnapshotMode;
    }

    /**
     * @param singleSnapshotMode the singleSnapshotMode to set
     */
    public void setSingleSnapshotMode(boolean singleSnapshotMode) {
        this.singleSnapshotMode = singleSnapshotMode;
    }

    /**
     * @return the enableDependencyMode
     */
    public boolean isEnableDependencyMode() {
        return enableDependencyMode;
    }

    /**
     * @param enableDependencyMode the enableDependencyMode to set
     */
    public void setEnableDependencyMode(boolean enableDependencyMode) {
        this.enableDependencyMode = enableDependencyMode;
    }

    /**
     * @return the codeUploadSetting
     */
    public CodeUploadSetting getCodeUploadSetting() {
        return codeUploadSetting;
    }

    /**
     * @param codeUploadSetting the codeUploadSetting to set
     */
    public void setCodeUploadSetting(String codeUploadSetting) {
        this.codeUploadSetting = CodeUploadSetting.valueOf(codeUploadSetting);
    }
    
    /**
     * @return the generateReports
     */
    public boolean isGenerateReports() {
        return generateReports;
    }

    /**
     * @param generateReports the generateReports to set
     */
    public void setGenerateReports(boolean generateReports) {
        this.generateReports = generateReports;
    }

    /**
     * @return the chosenServer
     */
    public String getChosenServer() {
        return chosenServer;
    }

    /**
     * @param chosenServer the chosenServer to set
     */
    public void setChosenServer(String chosenServer) {
        this.chosenServer = chosenServer;
    }

    /**
     * @return the sourceOrigin
     */
    public String getSourceOrigin() {
        return sourceOrigin;
    }

    /**
     * @param sourceOrigin the sourceOrigin to set
     */
    public void setSourceOrigin(String sourceOrigin) {
        this.sourceOrigin = sourceOrigin;
    }

    /**
     * @return the optionalReportTypes
     */
    public EnumSet<QARReportType> getChosenReportTypes() {
        return chosenReportTypes;
    }
    
    public void setChosenReportTypes(EnumSet<QARReportType> chosenReportTypes) {
        this.chosenReportTypes = chosenReportTypes;
    }

    /**
     * @return the enableDataFlowAnalysis
     */
    public boolean isEnableDataFlowAnalysis() {
        return enableDataFlowAnalysis;
    }

    /**
     * @param enableDataFlowAnalysis the enableDataFlowAnalysis to set
     */
    public void setEnableDataFlowAnalysis(boolean enableDataFlowAnalysis) {
        this.enableDataFlowAnalysis = enableDataFlowAnalysis;
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
                if(parsedValue < 0) {
                    return FormValidation.error(Messages.PRQANotifier_WrongDecimalValue());
                }
            } catch (NumberFormatException ex) {
                return FormValidation.error(Messages.PRQANotifier_WrongDecimalPunctuation());
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckProjectComplianceIndex(@QueryParameter String value) {
            try {
                Double parsedValue = Double.parseDouble(value);
                if(parsedValue < 0) {
                    return FormValidation.error(Messages.PRQANotifier_WrongDecimalValue());
                }
            } catch (NumberFormatException ex) {
                return FormValidation.error(Messages.PRQANotifier_WrongDecimalPunctuation());
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckTotalMax(@QueryParameter String value) {
            try {
                Integer parsedValue = Integer.parseInt(value);
                if(parsedValue < 0) {
                    return FormValidation.error(Messages.PRQANotifier_WrongInteger());
                }
            } catch (NumberFormatException ex) {
                return FormValidation.error(Messages.PRQANotifier_UseNoDecimals());
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckVcsConfigXml(@QueryParameter String value) {
            try {
                if(value.endsWith(".xml") || StringUtils.isBlank(value)) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(Messages.PRQANotifier_MustEndWithDotXml());
                }
            } catch (Exception ex) {
                return FormValidation.error(Messages.PRQANotifier_IllegalVcsString());
            }
        }
        
        public FormValidation doCheckMsgConfigFile(@QueryParameter String value) {
            try {
                if(value.endsWith(".xml") || StringUtils.isBlank(value)) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(Messages.PRQANotifier_MustEndWithDotXml());
                }
            } catch (Exception ex) {
                return FormValidation.error(Messages.PRQANotifier_IllegalVcsString());
            }
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
            //System.out.println(formData);
            JSONArray arr = formData.getJSONArray("chosenReport");
            QARReportType[] types = getOptionalReportTypes().toArray(new QARReportType[getOptionalReportTypes().size()]);
            
            instance.setChosenReportTypes(QARReportType.REQUIRED_TYPES.clone());
            
            for(int i=0; i<arr.size(); i++) {
                if(arr.getBoolean(i) == true) {
                    instance.chosenReportTypes.add(types[i]);
                }
            }
            instance.chosenReportTypes.add(QARReportType.Compliance);

            if(instance.getGraphTypes() == null || instance.getGraphTypes().isEmpty()) {
                ArrayList<PRQAGraph> list = new ArrayList<PRQAGraph>();                
                list.add(new ComplianceIndexGraphs());
                list.add(new MessagesGraph());
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
        
        public List<QACToolSuite> getQacTools() {
            QACToolSuite[] prqaInstallations = Hudson.getInstance().getDescriptorByType(QACToolSuite.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }
        
        public List<QACppToolSuite> getQacppTools() {
            QACppToolSuite[] prqaInstallations = Hudson.getInstance().getDescriptorByType(QACppToolSuite.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }
        
        public List<String> getComparisonSettings() {
            List<String> settings = new ArrayList<String>();
            for (ComparisonSettings setting : ComparisonSettings.values()) {
                settings.add(setting.toString());
            }
            return settings;
        }
        
        public List<QAVerifyServerConfiguration> getServers() {
            return PRQAGlobalConfig.get().getServers();
        }
        
        public CodeUploadSetting[] getUploadSettings() {
            return CodeUploadSetting.values();
        }
        
        public EnumSet<QARReportType> getOptionalReportTypes() {
            return QARReportType.OPTIONAL_TYPES;
        }

        public ListBoxModel doFillProductItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("QA·C", "qac");
            model.add("QA·C++", "qacpp");
            
            for(QACToolSuite suite : getQacTools()) {
                model.add(suite.getName());
            }          
            return model;            
        }
        
        public ListBoxModel doFillThreshholdlevelItems() {
            ListBoxModel model = new ListBoxModel();
            for(int i=0; i<10; i++) {
                model.add(""+i);
            }
            return model;
        }
    }
}
