package net.praqma.jenkins.plugin.prqa.setup;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.praqma.prqa.products.QACli;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QAFrameworkInstallationConfiguration extends ToolInstallation implements PRQAToolSuite, NodeSpecific<QAFrameworkInstallationConfiguration> {

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
		HashMap<String, String> environment = new HashMap<>();
		environment.put(QACli.QAF_INSTALL_PATH, getHome());
		environment.put(QACli.WORKSPACE_PATH, workspaceRoot);
		return environment;
	}

	public HashMap<String, String> convert(EnvVars vars) {
		HashMap<String, String> varsMap = new HashMap<>();
		for (Map.Entry<String, String> s : vars.entrySet()) {
			varsMap.put(s.getKey(), s.getValue());
		}
		return varsMap;
	}

	public static QAFrameworkInstallationConfiguration getInstallationByName(String qafName) {
		if (StringUtils.isBlank(qafName)) {
			return null;
		} else {
			Jenkins instance = Jenkins.getInstance();

			if (instance == null) {
				return null;
			}

			QAFrameworkInstallationConfiguration[] installations = instance
																		  .getDescriptorByType(QAFrameworkInstallationConfiguration.DescriptorImpl.class).getInstallations();
			for (QAFrameworkInstallationConfiguration install : installations) {
				if (install.getName().equals(qafName)) {
					return install;
				}
			}
		}
		return null;
	}

	@Override
	public QAFrameworkInstallationConfiguration forNode(Node node, TaskListener log) throws IOException, InterruptedException {
		String translatedHome = translateFor(node, log);
		return new QAFrameworkInstallationConfiguration(
				qafName,
				translatedHome,
				toolType,
				tool
		);
	}

	@Extension
	public static final class DescriptorImpl extends ToolDescriptor<QAFrameworkInstallationConfiguration> {

		public DescriptorImpl() {
			super();
			load();
		}

		@Override
		public String getDisplayName() {
			return "PRQA·Framework";
		}

		@Override
		public QAFrameworkInstallationConfiguration newInstance(StaplerRequest req, @Nonnull JSONObject formData) throws FormException {

			if (req == null) {
				throw new FormException(new Exception("Bad request"), "Bad request");
			}

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
				List<String> qaInstallationNames = new ArrayList<>();
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

			boolean configure = super.configure(req, json);
			if (configure) {
				save();
			}
			return configure;
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
            Jenkins instance = Jenkins.getInstance();

            if (instance == null) {
                return new QAFrameworkInstallationConfiguration[0];
            }

            return instance.getDescriptorByType(DescriptorImpl.class).getInstallations();
		}

		public FormValidation doCheckQafHome(@QueryParameter String qafHome) {
			if (StringUtils.isBlank(qafHome)) {
				return FormValidation.errorWithMarkup("PRQA·Framework Installation path should not be empty!");
			}
			if (qafHome.startsWith(" ")) {
				return FormValidation.errorWithMarkup("PRQA·Framework Installation path should not be begin with an empty space!");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckQafName(@QueryParameter String qafName) {
			if (StringUtils.isBlank(qafName)) {
				return FormValidation.errorWithMarkup("The name shall not be empty and shall be unique in the set of PRQA·Framework installations!");
			}
			if (qafName.startsWith(" ")) {
				return FormValidation.errorWithMarkup("PRQA·Framework Installation name should not be begin with an empty space!");
			}
			return FormValidation.ok();
		}
	}
}
