/*
 * The MIT License
 *
 * Copyright 2013 Praqma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.setup.QAFrameworkInstallationConfiguration;
import net.praqma.jenkins.plugin.prqa.threshold.AbstractThreshold;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
import java.util.List;

public class QAFrameworkPostBuildActionSetup extends PostBuildActionSetup {

    public String qaInstallation;
    public String qaProject;
    public String unifiedProjectName;
    public boolean useCustomLicenseServer;
    public String customLicenseServerAddress;
    public boolean downloadUnifiedProjectDefinition;
    public boolean performCrossModuleAnalysis;
    public String cmaProjectName;
    public boolean reuseCmaDb;
    public boolean useDiskStorage;
    public boolean enableDependencyMode;
    public boolean generateReport;
    public boolean publishToQAV;
    public boolean loginToQAV;
    public List<String> chosenServers;
    public boolean uploadWhenStable;
    public String qaVerifyProjectName;
    public String uploadSnapshotName;
    public String buildNumber;
    public String uploadSourceCode;
    public boolean generateCrr;
    public boolean generateMdr;
    public boolean generateSup;
    public boolean analysisSettings;
    public boolean stopWhenFail;
    public boolean customCpuThreads;
    public String maxNumThreads;
    public boolean generatePreprocess;
    public boolean assembleSupportAnalytics;
    public boolean generateReportOnAnalysisError;
    public boolean addBuildNumber;
    public String projectConfiguration;

    @DataBoundConstructor
    public QAFrameworkPostBuildActionSetup(String qaInstallation,
                                           String qaProject,
                                           boolean useCustomLicenseServer,
                                           String customLicenseServerAddress,
                                           boolean downloadUnifiedProjectDefinition,
                                           String unifiedProjectName,
                                           boolean performCrossModuleAnalysis,
                                           String cmaProjectName,
                                           boolean reuseCmaDb,
                                           boolean useDiskStorage,
                                           boolean enableDependencyMode,
                                           boolean generateReport,
                                           boolean publishToQAV,
                                           boolean loginToQAV,
                                           List<String> chosenServers,
                                           boolean uploadWhenStable,
                                           String qaVerifyProjectName,
                                           String uploadSnapshotName,
                                           String buildNumber,
                                           String uploadSourceCode,
                                           boolean generateCrr,
                                           boolean generateMdr,
                                           boolean generateSup,
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
        this.qaProject = qaProject;
        this.useCustomLicenseServer = useCustomLicenseServer;
        this.customLicenseServerAddress = customLicenseServerAddress;
        this.downloadUnifiedProjectDefinition = downloadUnifiedProjectDefinition;
        this.unifiedProjectName = unifiedProjectName;
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
        this.cmaProjectName = cmaProjectName;
        this.reuseCmaDb = reuseCmaDb;
        this.enableDependencyMode = enableDependencyMode;
        this.generateReport = generateReport;
        this.publishToQAV = publishToQAV;
        this.loginToQAV = loginToQAV;
        this.chosenServers = chosenServers;
        this.uploadWhenStable = uploadWhenStable;
        this.qaVerifyProjectName = qaVerifyProjectName;
        this.uploadSnapshotName = uploadSnapshotName;
        this.buildNumber = buildNumber;
        this.uploadSourceCode = uploadSourceCode;
        this.generateCrr = generateCrr;
        this.generateMdr = generateMdr;
        this.generateSup = generateSup;
        this.analysisSettings = analysisSettings;
        this.stopWhenFail = stopWhenFail;
        this.useDiskStorage = useDiskStorage;
        this.customCpuThreads = customCpuThreads;
        this.maxNumThreads = maxNumThreads;
        this.generatePreprocess = generatePreprocess;
        this.assembleSupportAnalytics = assembleSupportAnalytics;
        this.generateReportOnAnalysisError = generateReportOnAnalysisError;
        this.addBuildNumber = addBuildNumber;
        this.projectConfiguration = projectConfiguration;
    }

    public List<String> getChosenServers() {
        return chosenServers;
    }

    public void setChosenServers(List<String> chosenServers) {
        this.chosenServers = chosenServers;
    }

    public boolean isPublishToQAV() {
        return publishToQAV;
    }

    public boolean isLoginToQAV() {
        return loginToQAV;
    }

    public boolean isUploadWhenStable() {
        return uploadWhenStable;
    }

    public void setPublishToQAV(boolean publishToQAV) {
        this.publishToQAV = publishToQAV;
    }

    public void setLoginToQAV(boolean loginToQAV) {
        this.loginToQAV = loginToQAV;
    }

    public void setQaInstallation(String qaInstallation) {
        this.qaInstallation = qaInstallation;
    }

    public void setQaProject(String qaProject) {
        this.qaProject = qaProject;
    }

    public void setDownloadUnifiedProjectDefinition(boolean downloadUnifiedProjectDefinition) {
        this.downloadUnifiedProjectDefinition = downloadUnifiedProjectDefinition;
    }

    public void setPerformCrossModuleAnalysis(boolean performCrossModuleAnalysis) {
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
    }

    public void setEnableDependencyMode(boolean enableDependencyMode) {
        this.enableDependencyMode = enableDependencyMode;
    }

    public void setUploadWhenStable(boolean uploadWhenStable) {
        this.uploadWhenStable = uploadWhenStable;
    }

    public String getQaInstallation() {
        return qaInstallation;
    }

    public String getQaProject() {
        return qaProject;
    }

    public String getUnifiedProjectName() {
        return unifiedProjectName;
    }

    public String getVerifySnapshotName() {
        return uploadSnapshotName;
    }

    public void setUnifiedProjectName(String uProjectName) {
        unifiedProjectName = uProjectName;
    }

    public boolean isDownloadUnifiedProjectDefinition() {
        return downloadUnifiedProjectDefinition;
    }

    public boolean isPerformCrossModuleAnalysis() {
        return performCrossModuleAnalysis;
    }

    public boolean isEnableDependencyMode() {
        return enableDependencyMode;
    }

    public boolean isGenerateReport() {
        return generateReport;
    }

    public void setGenerateReport(boolean generateReport) {
        this.generateReport = generateReport;
    }

    public String getUploadSourceCode() {
        return uploadSourceCode;
    }

    public String getVerifyProjectName() {
        return qaVerifyProjectName;
    }

    public boolean isGenerateCrr() {
        return generateCrr;
    }

    public boolean isGenerateMdr() {
        return generateMdr;
    }

    public boolean isGenerateSup() {
        return generateSup;
    }

    public void setGenerateCrr(boolean generateCrr) {
        this.generateCrr = generateCrr;
    }

    public void setGenerateMdr(boolean generateMdr) {
        this.generateMdr = generateMdr;
    }

    public void setGenerateSup(boolean generateSup) {
        this.generateSup = generateSup;
    }

    public void setUploadSourceCode(String uploadSource) {
        uploadSourceCode = uploadSource;
    }

    public void setVerifyProjectName(String qaVerifyProject) {
        qaVerifyProjectName = qaVerifyProject;
    }

    public void setVerifySnapshotName(String qaVerifySnapshotName) {
        uploadSnapshotName = qaVerifySnapshotName;
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

    public void setReuseCmaDb(boolean reuseCmaDb) {
        this.reuseCmaDb = reuseCmaDb;
    }

    public void setUseDiskStorage(boolean useDiskStorage) {
        this.useDiskStorage = useDiskStorage;
    }

    public void setMaxNumThreads(String maxNumThreads) {
        this.maxNumThreads = maxNumThreads;
    }

    public void setCustomCpuThreads(boolean customCpuThreads) {
        this.customCpuThreads = customCpuThreads;
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

    public void setCmaProjectName(String cmaProjectName) {
        this.cmaProjectName = cmaProjectName;
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

    @Extension
    public final static class DescriptorImpl extends PRQAReportSourceDescriptor<QAFrameworkPostBuildActionSetup> {

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "PRQA·Framework";
        }

        public FormValidation doCheckCustomLicenseServerAddress(@QueryParameter String customLicenseServerAddress) {
            final String serverRegex = "^(\\d{1,5})@(.+)$";

            if (StringUtils.isBlank(customLicenseServerAddress)) {
                return FormValidation.error("Custom license server address must not be empty");
            } else if (!customLicenseServerAddress.matches(serverRegex)) {
                return FormValidation.error("License server format must be <port>@<host>");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckQAInstallation(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Error");
            } else {
                return FormValidation.ok();
            }
        }

        public ListBoxModel doFillUploadSourceCodeItems() {
            ListBoxModel SourceOption = new ListBoxModel();
            SourceOption.add("None", "NONE");
            SourceOption.add("All", "ALL");
            SourceOption.add("Only not in VCS", "NOT_IN_VCS");
            return SourceOption;
        }

        public FormValidation doCheckCmaProjectName(@QueryParameter String cmaProjectName) {
            if (StringUtils.isBlank(cmaProjectName)) {
                return FormValidation.errorWithMarkup("CMA project name should not be empty!");
            }
            if (!cmaProjectName.matches("^[a-zA-Z0-9_-{}()$%]+$")) {
                return FormValidation.errorWithMarkup("CMA project name is not valid [characters allowed: a-zA-Z0-9-_{}()$%]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUnifiedProjectName(@QueryParameter String unifiedProjectName) {
            if (StringUtils.isBlank(unifiedProjectName)) {
                return FormValidation.errorWithMarkup("Unified Project name should not be empty!");
            }
            if (!unifiedProjectName.matches("^[a-zA-Z0-9_-{}()$%]+$")) {
                return FormValidation.errorWithMarkup("Unified project name is not valid [characters allowed: a-zA-Z0-9-_{}()$%]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUploadSnapshotName(@QueryParameter String uploadSnapshotName) {
            if (StringUtils.isBlank(uploadSnapshotName)) {
                return FormValidation.ok();
            }
            if (!uploadSnapshotName.matches("^[a-zA-Z0-9_-{}()$%]+$")) {
                return FormValidation.errorWithMarkup("Snapshot name is not valid [characters allowed: a-zA-Z0-9-_{}()$%]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckQaVerifyProjectName(@QueryParameter String qaVerifyProjectName) {
            if (StringUtils.isBlank(qaVerifyProjectName)) {
                return FormValidation.errorWithMarkup("Project name should not be empty!");
            }
            if (!qaVerifyProjectName.matches("^[a-zA-Z0-9_-{}()$%]+$")) {
                return FormValidation.errorWithMarkup("Project name is not valid [characters allowed: a-zA-Z0-9-_{}()$%]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckMaxNumThreads(@QueryParameter String maxNumThreads) {
            final Integer minValue = 0;
            if (StringUtils.isBlank(maxNumThreads)) {
                return FormValidation.errorWithMarkup(Messages.PRQANotifier_NotEmptyValue("Max. Number of Threads for Analysis"));
            }
            try {
                final Integer parsedValue = Integer.parseInt(maxNumThreads);
                if (parsedValue <= minValue) {
                    return FormValidation.error(Messages.PRQANotifier_WrongIntegerGreatherThan(minValue));
                }
            } catch (NumberFormatException ex) {
                return FormValidation.error(Messages.PRQANotifier_UseInteger());
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillQaInstallationItems() {
            ListBoxModel model = new ListBoxModel();

            for (QAFrameworkInstallationConfiguration suiteQAFramework : getQAFrameworkTools()) {
                model.add(suiteQAFramework.getName());
            }
            return model;
        }

        public List<QAFrameworkInstallationConfiguration> getQAFrameworkTools() {
            Jenkins jenkins = Jenkins.getInstance();

            if (jenkins == null) {
                throw new RuntimeException("Unable to aquire Jenkins instance");
            }

            QAFrameworkInstallationConfiguration[] prqaInstallations = jenkins
                    .getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
            return Arrays.asList(prqaInstallations);
        }

        public List<ThresholdSelectionDescriptor<?>> getThresholdSelections() {
            return AbstractThreshold.getDescriptors();
        }

        public List<QAVerifyServerConfiguration> getServers() {
            return PRQAGlobalConfig.get().getServers();
        }

        public List<PRQAFileProjectSourceDescriptor<?>> getFileProjectSources() {
            return PRQAFileProjectSource.getDescriptors();
        }
    }
}
