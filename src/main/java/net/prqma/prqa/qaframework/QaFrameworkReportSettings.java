package net.prqma.prqa.qaframework;

import net.praqma.prqa.ReportSettings;

import java.io.Serializable;

public class QaFrameworkReportSettings
        implements ReportSettings, Serializable {

    private String qaInstallation;
    private String qaProject;
    private final boolean useCustomLicenseServer;
    private final String customLicenseServerAddress;
    private String uniProjectName;
    private boolean pullUnifiedProject;
    private boolean qaEnableDependencyMode;
    private boolean qaCrossModuleAnalysis;
    private String cmaProjectName;
    private boolean reuseCmaDb;
    private boolean useDiskStorage;
    private boolean generateReport;
    private boolean publishToQAV;
    private boolean loginToQAV;
    private boolean qaUploadWhenStable;
    private String qaVerifyProjectName;
    private String uploadSnapshotName;
    private String buildNumber;
    private String uploadSourceCode;
    private boolean genCrReport;
    private boolean genMdReport;
    private boolean genSupReport;
    private boolean analysisSettings;
    private boolean stopWhenFail;
    private boolean customCpuThreads;
    private String maxNumThreads;
    private boolean generatePreprocess;
    private boolean assembleSupportAnalytics;
    private boolean generateReportOnAnalysisError;
    private boolean addBuildNumber;
    private String projectConfiguration;

    public QaFrameworkReportSettings(String qaInstallation,
                                     boolean useCustomLicenseServer,
                                     String customLicenseServerAddress,
                                     String qaProject,
                                     boolean pullUnifiedProject,
                                     String uniProjectName,
                                     boolean qaEnableDependencyMode,
                                     boolean qaCrossModuleAnalysis,
                                     String cmaProjectName,
                                     boolean reuseCmaDb,
                                     boolean useDiskStorage,
                                     boolean generateReport,
                                     boolean publishToQAV,
                                     boolean loginToQAV,
                                     boolean qaUploadWhenStable,
                                     String qaVerifyProjectName,
                                     String uploadSnapshotName,
                                     String buildNumber,
                                     String uploadSourceCode,
                                     boolean genCrReport,
                                     boolean genMdReport,
                                     boolean genSupReport,
                                     boolean analysisSettings,
                                     boolean stopWhenFail,
                                     boolean customCpuThreads,
                                     String maxNumThreads,
                                     boolean generatePreprocess,
                                     boolean assembleSupportAnalytics,
                                     boolean generateReportOnAnalysisError,
                                     boolean addBuildNumber,
                                     String projectConfiguration) {

        this.qaInstallation = qaInstallation;
        this.useCustomLicenseServer = useCustomLicenseServer;
        this.customLicenseServerAddress = customLicenseServerAddress;
        this.uniProjectName = uniProjectName;
        this.pullUnifiedProject = pullUnifiedProject;
        this.qaCrossModuleAnalysis = qaCrossModuleAnalysis;
        this.cmaProjectName = cmaProjectName;
        this.reuseCmaDb = reuseCmaDb;
        this.useDiskStorage = useDiskStorage;
        this.publishToQAV = publishToQAV;
        this.loginToQAV = loginToQAV;
        this.qaEnableDependencyMode = qaEnableDependencyMode;
        this.generateReport = generateReport;
        this.qaProject = qaProject;
        this.qaUploadWhenStable = qaUploadWhenStable;
        this.qaVerifyProjectName = qaVerifyProjectName;
        this.uploadSnapshotName = uploadSnapshotName;
        this.buildNumber = buildNumber;
        this.uploadSourceCode = uploadSourceCode;
        this.genMdReport = genMdReport;
        this.genCrReport = genCrReport;
        this.genSupReport = genSupReport;
        this.analysisSettings = analysisSettings;
        this.stopWhenFail = stopWhenFail;
        this.customCpuThreads = customCpuThreads;
        this.maxNumThreads = maxNumThreads;
        this.generatePreprocess = generatePreprocess;
        this.assembleSupportAnalytics = assembleSupportAnalytics;

        this.generateReportOnAnalysisError = generateReportOnAnalysisError;
        this.addBuildNumber = addBuildNumber;
        this.projectConfiguration = projectConfiguration;

    }

    @Override
    public String getProduct() {
        return "";
    }

    @Override
    public boolean publishToQAV() {
        return publishToQAV;
    }

    public boolean loginToQAV() {
        return loginToQAV;
    }

    public String getQaInstallation() {
        return qaInstallation;
    }

    public String getQaProject() {
        return qaProject;
    }

    public boolean isQaEnableDependencyMode() {
        return qaEnableDependencyMode;
    }

    public boolean isQaCrossModuleAnalysis() {
        return qaCrossModuleAnalysis;
    }

    public boolean isPullUnifiedProject() {
        return pullUnifiedProject;
    }

    public String getUniProjectName() {
        return uniProjectName;
    }

    public boolean isGenerateReport() {
        return generateReport;
    }

    public boolean isPublishToQAV() {
        return publishToQAV;
    }

    public boolean isLoginToQAV() {
        return loginToQAV;
    }

    public boolean isQaUploadWhenStable() {
        return qaUploadWhenStable;
    }

    public String getQaVerifyProjectName() {
        return qaVerifyProjectName;
    }

    public void setQaVerifyProjectName(String qaVerifyProjectName) {
        this.qaVerifyProjectName = qaVerifyProjectName;
    }

    public String getUploadSnapshotName() {
        return uploadSnapshotName;
    }

    public void setUploadSnapshotName(String uploadSnapshotName) {
        this.uploadSnapshotName = uploadSnapshotName;
    }

    public String getbuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getUploadSourceCode() {
        return uploadSourceCode;
    }

    public void setUploadSourceCode(String uploadSourceCode) {
        this.uploadSourceCode = uploadSourceCode;
    }

    public boolean isGenCrReport() {
        return genCrReport;
    }

    public boolean isGenMdReport() {
        return genMdReport;
    }

    public boolean isGenSupReport() {
        return genSupReport;
    }

    public void setGenCrReport(boolean genCrReport) {
        this.genCrReport = genCrReport;
    }

    public void setGenMdReport(boolean genMdReport) {
        this.genMdReport = genMdReport;
    }

    public void setGenSupReport(boolean genSupReport) {
        this.genSupReport = genSupReport;
    }

    public boolean isAnalysisSettings() {
        return analysisSettings;
    }

    public boolean isStopWhenFail() {
        return stopWhenFail;
    }

    public boolean isGeneratePreprocess() {
        return generatePreprocess;
    }

    public boolean isAssembleSupportAnalytics() {
        return assembleSupportAnalytics;
    }

    public void setAnalysisSettings(boolean analysisSettings) {
        this.analysisSettings = analysisSettings;
    }

    public void setStopWhenFail(boolean stopWhenFail) {
        this.stopWhenFail = stopWhenFail;
    }

    public void setGeneratePreprocess(boolean generatePreprocess) {
        this.generatePreprocess = generatePreprocess;
    }

    public void setAssembleSupportAnalytics(boolean assembleSupportAnalytics) {
        this.assembleSupportAnalytics = assembleSupportAnalytics;
    }

    public String getCustomLicenseServerAddress() {
        return customLicenseServerAddress;
    }

    public boolean isUseCustomLicenseServer() {
        return useCustomLicenseServer;
    }

    public boolean isGenerateReportOnAnalysisError() {
        return generateReportOnAnalysisError;
    }

    public boolean isReuseCmaDb() {
        return reuseCmaDb;
    }

    public boolean isUseDiskStorage() {
        return useDiskStorage;
    }

    public String getMaxNumThreads() {
        return maxNumThreads;
    }

    public boolean isCustomCpuThreads() {
        return customCpuThreads;
    }

    public String getCmaProjectName() {
        return cmaProjectName;
    }

    public boolean isAddBuildNumber() {
        return addBuildNumber;
    }

    public void setAddBuildNumber(boolean addBuildNumber) {
        this.addBuildNumber = addBuildNumber;
    }

    public String getProjectConfiguration() {
        return projectConfiguration;
    }

    public void setProjectConfiguration(String projectConfiguration) {
        this.projectConfiguration = projectConfiguration;
    }
}
