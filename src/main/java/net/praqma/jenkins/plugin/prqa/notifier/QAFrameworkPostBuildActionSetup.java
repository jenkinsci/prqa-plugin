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
import hudson.model.Hudson;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.Arrays;
import java.util.List;

import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.setup.QAFrameworkInstallationConfiguration;
import net.praqma.jenkins.plugin.prqa.threshold.AbstractThreshold;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class QAFrameworkPostBuildActionSetup extends PostBuildActionSetup {

    public String qaInstallation;
    public String qaProject;
    public String unifiedProjectName;
    public boolean downloadUnifiedProjectDefinition;
    public boolean performCrossModuleAnalysis;
    public String CMAProjectName;
    public boolean enableMtr;
    public boolean enableProjectCma;
    public boolean enableDependencyMode;
    public boolean generateReport;
    public boolean publishToQAV;
    public boolean loginToQAV;
    public String chosenServer;
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
    public boolean generatePreprocess;
    public boolean assembleSupportAnalytics;

    @DataBoundConstructor
    public QAFrameworkPostBuildActionSetup(String qaInstallation, String qaProject, boolean downloadUnifiedProjectDefinition, String unifiedProjectName,
            boolean performCrossModuleAnalysis, String CMAProjectName, boolean enableMtr, boolean enableProjectCma, boolean enableDependencyMode,
            boolean generateReport, boolean publishToQAV, boolean loginToQAV, String chosenServer, boolean uploadWhenStable, String qaVerifyProjectName,
            String uploadSnapshotName, String buildNumber, String uploadSourceCode, boolean generateCrr, boolean generateMdr, boolean generateSup,
            boolean analysisSettings, boolean stopWhenFail, boolean generatePreprocess, boolean assembleSupportAnalytics) {

        this.qaInstallation = qaInstallation;
        this.qaProject = qaProject;
        this.downloadUnifiedProjectDefinition = downloadUnifiedProjectDefinition;
        this.unifiedProjectName = unifiedProjectName;
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
        this.CMAProjectName = CMAProjectName;
        this.enableMtr = enableMtr;
        this.enableProjectCma = enableProjectCma;
        this.enableDependencyMode = enableDependencyMode;
        this.generateReport = generateReport;
        this.publishToQAV = publishToQAV;
        this.loginToQAV = loginToQAV;
        this.chosenServer = chosenServer;
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
        this.generatePreprocess = generatePreprocess;
        this.assembleSupportAnalytics = assembleSupportAnalytics;
    }

    public String getChosenServer() {
        return chosenServer;
    }

    public void setChosenServer(String chosenServer) {
        this.chosenServer = chosenServer;
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

    public void setEnableMtr(boolean enableMtr) {
        this.enableMtr = enableMtr;
    }

    public void setUploadWhenStable(boolean uploadWhenStable) {
        this.uploadWhenStable = uploadWhenStable;
    }

    public void setEnableProjectCma(boolean enableProjectCma) {
        this.enableProjectCma = enableProjectCma;
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

    public String getCMAProjectName() {
        return CMAProjectName;
    }

    public void setCMAProjectName(String cMAProjectName) {
        CMAProjectName = cMAProjectName;
    }

    public boolean isPerformCrossModuleAnalysis() {
        return performCrossModuleAnalysis;
    }

    public boolean isEnableDependencyMode() {
        return enableDependencyMode;
    }

    public boolean isEnableMtr() {
        return enableMtr;
    }

    public boolean isEnableProjectCma() {
        return enableProjectCma;
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

    @Extension
    public final static class DescriptorImpl extends PRQAReportSourceDescriptor<QAFrameworkPostBuildActionSetup> {

        public boolean enabled = false;

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

        public FormValidation doCheckQAInstallation(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Error");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckCMAProjectName(@QueryParameter String CMAProjectName) {
            if (StringUtils.isBlank(CMAProjectName)) {
                return FormValidation.errorWithMarkup("CMA project name should not be empty!");
            }
            if (!CMAProjectName.matches("^[a-zA-Z0-9_-]+$")) {
                return FormValidation.errorWithMarkup("CMA project name is not valid [characters allowed: a-zA-Z0-9-_]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUnifiedProjectName(@QueryParameter String unifiedProjectName) {
            if (StringUtils.isBlank(unifiedProjectName)) {
                return FormValidation.errorWithMarkup("Unified Project name should not be empty!");
            }
            if (!unifiedProjectName.matches("^[a-zA-Z0-9_-]+$")) {
                return FormValidation.errorWithMarkup("Unified project name is not valid [characters allowed: a-zA-Z0-9-_]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUploadSnapshotName(@QueryParameter String uploadSnapshotName) {
            if (StringUtils.isBlank(uploadSnapshotName)) {
                return FormValidation.errorWithMarkup("Snapshot name should not be empty!");
            }
            if (!uploadSnapshotName.matches("^[a-zA-Z0-9_-]+$")) {
                return FormValidation.errorWithMarkup("Snapshot name is not valid [characters allowed: a-zA-Z0-9-_]");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckQaVerifyProjectName(@QueryParameter String qaVerifyProjectName) {
            if (StringUtils.isBlank(qaVerifyProjectName)) {
                return FormValidation.errorWithMarkup("Project name should not be empty!");
            }
            if (!qaVerifyProjectName.matches("^[a-zA-Z0-9_-]+$")) {
                return FormValidation.errorWithMarkup("Project name is not valid [characters allowed: a-zA-Z0-9-_]");
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
            QAFrameworkInstallationConfiguration[] prqaInstallations = Hudson.getInstance()
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
