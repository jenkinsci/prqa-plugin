package net.praqma.jenkins.plugin.prqa.setup;

import hudson.EnvVars;
import hudson.Extension;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jenkins.model.Jenkins;
import net.praqma.prqa.products.QACli;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Strings;

public class QAFrameworkInstallationConfiguration extends ToolInstallation implements PRQAToolSuite {

	private static final long serialVersionUID = 1L;
	public final String qafHome;
	public final String qafName;
	public final String toolType;
	public final String tool;

	@DataBoundConstructor
	public QAFrameworkInstallationConfiguration(final String qafName, final String qafHome, final String tool, final String toolType) {
		super(qafName, qafHome, null);

		this.qafHome = qafHome;
		this.qafName = qafName;
		this.toolType = toolType;
		this.tool = tool;
	}

	@Override
	public HashMap<String, String> createEnvironmentVariables(String workspaceRoot) {
		HashMap<String, String> environment = new HashMap<String, String>();
		environment.put(QACli.QAF_INSTALL_PATH, getHome());
		environment.put(QACli.WORKSPACE_PATH, workspaceRoot);
		return environment;
	}

	public HashMap<String, String> convert(EnvVars vars) {
		HashMap<String, String> varsMap = new HashMap<String, String>();
		for (String s : vars.keySet()) {
			varsMap.put(s, vars.get(s));
		}
		return varsMap;
	}

	public static QAFrameworkInstallationConfiguration getInstallationByName(String qafName) {
		if (StringUtils.isBlank(qafName)) {
			return null;
		} else {
			QAFrameworkInstallationConfiguration[] installations = Jenkins.getInstance()
					.getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
			for (QAFrameworkInstallationConfiguration install : installations) {
				if (install.getName().equals(qafName)) {
					return install;
				}
			}
		}
		return null;
	}

	@Extension
	public static final class DescriptorImpl extends ToolDescriptor<QAFrameworkInstallationConfiguration> {

		public DescriptorImpl() {
			super();
			load();
		}

		@Override
		public String getDisplayName() {
			return "QA·Framework";
		}

		@Override
		public QAFrameworkInstallationConfiguration newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			QAFrameworkInstallationConfiguration suite = req.bindJSON(QAFrameworkInstallationConfiguration.class, formData);

			save();
			return suite;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			String qafHome = "qafHome";
			String qafName = "qafName";
			String tool = "tool";

			if (!json.isEmpty()) {
				List<String> qaInstallationNames = new ArrayList<String>();
				if (json.get(tool) instanceof JSONArray) {
					JSONArray array = json.getJSONArray(tool);
					Iterator it = array.iterator();
					while (it.hasNext()) {
						JSONObject jsonObject = (JSONObject) it.next();
						String qaName = jsonObject.getString(qafName);
						String qaHome = jsonObject.getString(qafHome);
						if (Strings.isNullOrEmpty(qaName) || Strings.isNullOrEmpty(qaHome) || qaInstallationNames.contains(qaName) || !isValidString(qaName)
								|| !isValidString(qaHome)) {
							it.remove();
						} else {
							qaInstallationNames.add(qaName);
						}
					}
					if (array.isEmpty()) {
						json = new JSONObject();
					}
				} else {
					JSONObject jsonObject = json.getJSONObject(tool);
					String qaName = jsonObject.getString(qafName);
					String qaHome = jsonObject.getString(qafHome);
					if (Strings.isNullOrEmpty(qaName) || Strings.isNullOrEmpty(qaHome) || !isValidString(qaName) || !isValidString(qaHome)) {
						json = new JSONObject();
					}
				}
			}
			save();
			return super.configure(req, json);
		}

		private boolean isValidString(String valid) {
			boolean isValid = true;
			String trim = valid.trim();
			if (Strings.isNullOrEmpty(trim) || !valid.equals(trim) || trim.length() < 1)
				isValid = false;
			return isValid;
		}

		public ListBoxModel doFillQafNameItems() {
			ListBoxModel model = new ListBoxModel();
			for (QAFrameworkInstallationConfiguration suite : getQaInstallations()) {
				model.add(suite.getName());
			}
			return model;
		}

		public QAFrameworkInstallationConfiguration[] getQaInstallations() {
			QAFrameworkInstallationConfiguration[] installations = Jenkins.getInstance()
					.getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
			return installations;
		}

		public FormValidation doCheckQafHome(@QueryParameter String qafHome) {
			if (StringUtils.isBlank(qafHome)) {
				return FormValidation.errorWithMarkup("QA·Framework Installation path should not be empty!");
			}
			if (qafHome.startsWith(" ")) {
				return FormValidation.errorWithMarkup("QA·Framework Installation path should not be begin with an empty space!");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckQafName(@QueryParameter String qafName) {
			if (StringUtils.isBlank(qafName)) {
				return FormValidation.errorWithMarkup("The name shall not be empty and shall be unique in the set of QA·Framework installations!");
			}
			if (qafName.startsWith(" ")) {
				return FormValidation.errorWithMarkup("QA·Framework Installation name should not be begin with an empty space!");
			}
			return FormValidation.ok();
		}
	}
}
