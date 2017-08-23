/*
 * The MIT License
 *
 * Copyright 2013 mads.
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
package net.praqma.jenkins.plugin.prqa.threshold;

import hudson.Extension;
import hudson.util.FormValidation;
import net.praqma.jenkins.plugin.prqa.notifier.Messages;
import net.praqma.jenkins.plugin.prqa.notifier.ThresholdSelectionDescriptor;
import net.praqma.prqa.status.PRQAComplianceStatus;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * 
 * @author mads
 */
public class ProjectComplianceThreshold extends AbstractThreshold {

	public final Double value;

	@DataBoundConstructor
	public ProjectComplianceThreshold(final Double value, final Boolean improvement) {
		super(improvement);
		this.value = value;
	}

	@Override
	public boolean validateImprovement(PRQAComplianceStatus previousComplianceStatus, PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
		boolean isValidImprovement = true;
		if (previousComplianceStatus != null) {
			isValidImprovement = currentComplianceStatus.getProjectCompliance() >= previousComplianceStatus.getProjectCompliance();
			if (!isValidImprovement) {
				currentComplianceStatus.addNotification(Messages.PRQANotifier_ProjectComplianceIndexRequirementNotMet(
						currentComplianceStatus.getProjectCompliance(), previousComplianceStatus.getProjectCompliance()));
			}
		}
		return isValidImprovement;
	}

	@Override
	public boolean validateThreshold(PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
		boolean isValidTreshold = true;
		if (value != null) {
			isValidTreshold = currentComplianceStatus.getProjectCompliance() >= value;
			if (!isValidTreshold) {
				currentComplianceStatus.addNotification(Messages.PRQANotifier_ProjectComplianceIndexRequirementNotMet(
						currentComplianceStatus.getProjectCompliance(), value));
			}
		}
		return isValidTreshold;
	}

	@Extension
	public static final class DescriptorImpl extends ThresholdSelectionDescriptor<ProjectComplianceThreshold> {

		@Override
		public String getDisplayName() {
			return "Project Compliance Threshold";
		}

		@Override
		public String getHelpFile() {
			return "/plugin/prqa-plugin/config/help-thresholds.html";
		}
	}
}
