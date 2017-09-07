package net.praqma.jenkins.plugin.prqa.notifier;

import com.google.common.collect.Iterables;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.Descriptor;
import hudson.model.ProminentProjectAction;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.RunList;
import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Praqma
 */
public class PRQAProjectAction extends Actionable implements ProminentProjectAction {

	private final AbstractProject<?, ?> project;
	private static final String ICON_NAME = "/plugin/prqa-plugin/images/32x32/prqa.png";

	public PRQAProjectAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	@Override
	public String getDisplayName() {
		return "PRQA Results";
	}

	@Override
	public String getSearchUrl() {
		return "PRQA";
	}

	@Override
	public String getIconFileName() {
		return ICON_NAME;
	}

	@Override
	public String getUrlName() {
		return "PRQA";
	}

	public PRQABuildAction getLatestActionInProject() {
		if (project.getLastSuccessfulBuild() != null) {
			return project.getLastSuccessfulBuild().getAction(PRQABuildAction.class);
		}
		return null;
	}

	/**
	 * Small method to determine whether to draw graphs or not.
	 * 
	 * @return true when there are more than 2 or more builds available.
	 */
	public boolean isDrawGraphs() {
		RunList<?> builds = project.getBuilds();
		return Iterables.size(builds) >= 2;
	}

	/**
	 * New one.
	 * 
	 * @param req
	 * @param rsp
	 */
	public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) {
		PRQABuildAction action = getLatestActionInProject();
		if (action != null) {
			try {
				action.doReportGraphs(req, rsp);
			} catch (IOException exception) {

			}
		}
	}

	public QAVerifyServerConfiguration getConfiguration() {
		DescribableList<Publisher, Descriptor<Publisher>> publishersList = project.getPublishersList();
		PRQANotifier notifier = publishersList.get(PRQANotifier.class);
		if (notifier != null) {
			QAVerifyServerConfiguration qavconfig;
			if (notifier.sourceQAFramework instanceof PRQAReportPRQAToolSource) {
				List<String> servers = ((PRQAReportPRQAToolSource) notifier.sourceQAFramework).chosenServers;
				String chosenServer = servers != null && !servers.isEmpty() ? servers.get(0) : null;
				qavconfig = PRQAGlobalConfig.get().getConfigurationByName(chosenServer);
			} else {
				List<String> servers = ((QAFrameworkPostBuildActionSetup) notifier.sourceQAFramework).chosenServers;
				String chosenServer = servers != null && !servers.isEmpty() ? servers.get(0) : null;
				qavconfig = PRQAGlobalConfig.get().getConfigurationByName(chosenServer);
			}
			return qavconfig;
		}
		return null;
	}

	public Collection<QAVerifyServerConfiguration> getConfigurations() {
		DescribableList<Publisher, Descriptor<Publisher>> publishersList = project.getPublishersList();
		PRQANotifier notifier = publishersList.get(PRQANotifier.class);
		if (notifier != null) {
			Collection<QAVerifyServerConfiguration> qavconfig;
			if (notifier.sourceQAFramework instanceof PRQAReportPRQAToolSource) {
				qavconfig = PRQAGlobalConfig.get().getConfigurationsByNames(((PRQAReportPRQAToolSource) notifier.sourceQAFramework).chosenServers);
			} else {
				qavconfig = PRQAGlobalConfig.get().getConfigurationsByNames(((QAFrameworkPostBuildActionSetup) notifier.sourceQAFramework).chosenServers);
			}
			return qavconfig;
		}
		return null;
	}
}
