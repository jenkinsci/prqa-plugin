package net.praqma.jenkins.plugin.prqa.notifier;

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

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ListBoxModel;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import net.praqma.jenkins.plugin.prqa.setup.QACToolSuite;
import net.praqma.prqa.CodeUploadSetting;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

//import net.praqma.prqa.CodeUploadSetting;

/**
 * 
 * @author Praqma
 */
public class PRQAReportPRQAToolSource extends PostBuildActionSetup {

	public String product;
	public String projectFile;
	public boolean performCrossModuleAnalysis;
	public boolean enableDependencyMode;
	public boolean enableDataFlowAnalysis;
	public PRQAFileProjectSource fileProjectSource;

	// publish to qav

	public final boolean publishToQAV;
	public String chosenServer;
	public CodeUploadSetting codeUploadSetting = CodeUploadSetting.None;
	public String qaVerifyProjectName;
	public String vcsConfigXml;
	public String sourceOrigin;
	public boolean singleSnapshotMode;

	@DataBoundConstructor
	public PRQAReportPRQAToolSource(

	String product, String projectFile, boolean performCrossModuleAnalysis, boolean enableDependencyMode, boolean enableDataFlowAnalysis,
			PRQAFileProjectSource fileProjectSource, boolean publishToQAV, String chosenServer, String codeUploadSetting, String qaVerifyProjectName,
			String vcsConfigXml, String sourceOrigin, boolean singleSnapshotMode) {

		this.product = product;
		this.projectFile = projectFile;
		this.performCrossModuleAnalysis = performCrossModuleAnalysis;
		this.enableDependencyMode = enableDependencyMode;
		this.enableDataFlowAnalysis = enableDataFlowAnalysis;
		this.fileProjectSource = fileProjectSource;
		// publish to qav
		this.publishToQAV = publishToQAV;
		this.chosenServer = chosenServer;
		this.codeUploadSetting = CodeUploadSetting.getByValue(codeUploadSetting);
		this.qaVerifyProjectName = qaVerifyProjectName;
		this.vcsConfigXml = vcsConfigXml;
		this.sourceOrigin = sourceOrigin;
		this.singleSnapshotMode = singleSnapshotMode;

	}

	public boolean isPublishToQAV() {
		return publishToQAV;
	}

//	public void setPublishToQAV(boolean publishToQAV) {
//		this.publishToQAV = publishToQAV;
//	}

	public String getChosenServer() {
		return chosenServer;
	}

	public void setChosenServer(String chosenServer) {
		this.chosenServer = chosenServer;
	}

	public CodeUploadSetting getCodeUploadSetting() {
		return codeUploadSetting;
	}

	public void setCodeUploadSetting(CodeUploadSetting codeUploadSetting) {
		this.codeUploadSetting = codeUploadSetting;
	}

	public String getQaVerifyProjectName() {
		return qaVerifyProjectName;
	}

	public void setQaVerifyProjectName(String qaVerifyProjectName) {
		this.qaVerifyProjectName = qaVerifyProjectName;
	}

	public String getVcsConfigXml() {
		return vcsConfigXml;
	}

	public void setVcsConfigXml(String vcsConfigXml) {
		this.vcsConfigXml = vcsConfigXml;
	}

	public String getSourceOrigin() {
		return sourceOrigin;
	}

	public void setSourceOrigin(String sourceOrigin) {
		this.sourceOrigin = sourceOrigin;
	}

	public boolean isSingleSnapshotMode() {
		return singleSnapshotMode;
	}

	public void setSingleSnapshotMode(boolean singleSnapshotMode) {
		this.singleSnapshotMode = singleSnapshotMode;
	}

	public String getProjectFile() {
		return projectFile;
	}

	public String getProduct() {
		return product;
	}

	public boolean isPerformCrossModuleAnalysis() {
		return performCrossModuleAnalysis;
	}

	public boolean isEnableDependencyMode() {
		return enableDependencyMode;
	}

	public boolean isEnableDataFlowAnalysis() {
		return enableDataFlowAnalysis;
	}

	public PRQAFileProjectSource getFileProjectSource() {
		return fileProjectSource;
	}

	public void setProjectFile(String projectFile) {
		this.projectFile = projectFile;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public void setPerformCrossModuleAnalysis(boolean performCrossModuleAnalysis) {
		this.performCrossModuleAnalysis = performCrossModuleAnalysis;
	}

	public void setEnableDependencyMode(boolean enableDependencyMode) {
		this.enableDependencyMode = enableDependencyMode;
	}

	public void setEnableDataFlowAnalysis(boolean enableDataFlowAnalysis) {
		this.enableDataFlowAnalysis = enableDataFlowAnalysis;
	}

	public void setFileProjectSource(PRQAFileProjectSource fileProjectSource) {
		this.fileProjectSource = fileProjectSource;
	}

	@Extension
	public final static class DescriptorImpl extends PRQAReportSourceDescriptor<PRQAReportPRQAToolSource> {

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
			save();
			return super.configure(req, json);
		}

		public EnumSet<QARReportType> getOptionalReportTypes() {
			return QARReportType.OPTIONAL_TYPES;
		}

		public List<PRQAFileProjectSourceDescriptor<?>> getFileProjectSources() {
			return PRQAFileProjectSource.getDescriptors();
		}

		public DescriptorImpl() {
			super();
			load();
		}

		public ListBoxModel doFillProductItems() {

			ListBoxModel model = new ListBoxModel();

			for (QACToolSuite suiteQAFramework : getQAFrameworkTools()) {
				model.add(suiteQAFramework.getName());
			}
			return model;
		}

		public List<QACToolSuite> getQAFrameworkTools() {
			QACToolSuite[] prqaInstallations = Hudson.getInstance().getDescriptorByType(QACToolSuite.DescriptorImpl.class).getInstallations();
			return Arrays.asList(prqaInstallations);
		}

		@Override
		public String getDisplayName() {
			return "Existing PRQA Tool";
		}
	
		public List<QAVerifyServerConfiguration> getServers() {
			return PRQAGlobalConfig.get().getServers();
		}
	
		public CodeUploadSetting[] getUploadSettings() {
			return CodeUploadSetting.values();
		}
		
	}
}