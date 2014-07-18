package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.io.IOException;

import net.praqma.jenkins.plugin.prqa.globalconfig.PRQAGlobalConfig;
import net.praqma.jenkins.plugin.prqa.globalconfig.QAVerifyServerConfiguration;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

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
	 * Small method to determin whether to draw graphs or not.
	 * 
	 * @return true when there are more than 2 or more builds available.
	 */
	public boolean isDrawGraphs() {
		return project.getBuilds().size() >= 2;
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
				qavconfig = PRQAGlobalConfig.get().getConfigurationByName(((PRQAReportPRQAToolSource) notifier.sourceQAFramework).chosenServer);
			} else {
				qavconfig = PRQAGlobalConfig.get().getConfigurationByName(((QAFrameworkPostBuildActionSetup) notifier.sourceQAFramework).chosenServer);
			}
			return qavconfig;
		}
		return null;
	}
}
