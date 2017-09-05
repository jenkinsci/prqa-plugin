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
import net.praqma.prqa.parsers.MessageGroup;
import net.praqma.prqa.status.PRQAComplianceStatus;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public class MessageComplianceThreshold extends AbstractThreshold {

    public final Integer value;
    private static final Logger log = Logger.getLogger(MessageComplianceThreshold.class.getName());

    @DataBoundConstructor
    public MessageComplianceThreshold(final Integer value, final int thresholdLevel, final Boolean improvement) {
        super(improvement);
        this.value = value;
    }

    @Override
    public boolean validateImprovement(PRQAComplianceStatus previousComplianceStatus, PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
        if (value == null) {
            return true;
        }
        return isImprovementForMessageGroups(previousComplianceStatus, currentComplianceStatus, thresholdLevel);
    }

    public boolean isImprovementForMessageGroups(PRQAComplianceStatus previousComplianceStatus, PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
        boolean areAllMessagesValid = true;
        List<MessageGroup> currentComplianceStatusMessageGroups = currentComplianceStatus.getMessagesGroups();

        if (currentComplianceStatusMessageGroups == null || currentComplianceStatusMessageGroups.isEmpty()) {
            areAllMessagesValid = currentComplianceStatus.getMessageCount(thresholdLevel) <= previousComplianceStatus.getMessageCount(thresholdLevel);
            if (!areAllMessagesValid) {
                currentComplianceStatus.addNotification(Messages.PRQANotifier_MaxMessagesRequirementNotMetExistingPrqa(currentComplianceStatus.getMessageCount(thresholdLevel), previousComplianceStatus.getMessageCount(thresholdLevel)));
            }
        } else {
            List<MessageGroup> previousComplianceStatusMessageGroups = previousComplianceStatus.getMessagesGroups();
            for (int i = 0; i < currentComplianceStatusMessageGroups.size(); i++) {
                MessageGroup currentMessageGroup = currentComplianceStatusMessageGroups.get(i);
                for (int j = 0; j < previousComplianceStatusMessageGroups.size(); j++) {
                    if (currentMessageGroup.getMessageGroupName().equals(previousComplianceStatusMessageGroups.get(j).getMessageGroupName())) {
                        if (currentMessageGroup.getMessagesWithinThreshold() <= previousComplianceStatusMessageGroups.get(j).getMessagesWithinThreshold()) {
                            currentComplianceStatus.addNotification(onUnstableMessage(currentMessageGroup.getMessageGroupName(),
                                    currentMessageGroup.getMessagesWithinThreshold(), previousComplianceStatusMessageGroups.get(j).getMessagesWithinThreshold()));
                            areAllMessagesValid = false;
                        }
                    }
                }
            }
        }
        return areAllMessagesValid;
    }

    private String onUnstableMessage(String messageGroupName, int actualValue, int comparisonValue) {
        return Messages.PRQANotifier_MaxMessagesRequirementNotMet(messageGroupName, actualValue, comparisonValue);
    }

    @Override
    public boolean validateThreshold(PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
        if (value == null) {
            return true;
        }
        return isTresholdValidForMessageGroups(currentComplianceStatus, thresholdLevel);
    }

    private boolean isTresholdValidForMessageGroups(PRQAComplianceStatus currentComplianceStatus, int thresholdLevel) {
        boolean isValidTreshold = true;
        boolean isStableBuild = true;
        List<MessageGroup> messageGroups = currentComplianceStatus.getMessagesGroups();
        if (messageGroups == null || messageGroups.isEmpty()) {
            isValidTreshold = currentComplianceStatus.getMessageCount(thresholdLevel) <= value;
            if (!isValidTreshold) {
                currentComplianceStatus.addNotification(Messages.PRQANotifier_MaxMessagesRequirementNotMetExistingPrqa(
                        currentComplianceStatus.getMessageCount(thresholdLevel), value));
            }
        } else {
            for (MessageGroup messageGroup : currentComplianceStatus.getMessagesGroups()) {
                isValidTreshold = messageGroup.getMessagesWithinThreshold() <= value;
                if (!isValidTreshold) {
                    currentComplianceStatus.addNotification(onUnstableMessage(messageGroup.getMessageGroupName(), messageGroup.getMessagesWithinThreshold(), value));
                    isStableBuild = false;
                }
                log.fine(String.format("For %s are %s mesages, comparing to: %s", messageGroup.getMessageGroupName(), messageGroup.getMessagesWithinThreshold(), value));
                log.fine(String.format("ValidateThreshold returned %s", isValidTreshold));
            }
        }
        return isStableBuild;
    }

    @Extension
    public static final class DescriptorImpl extends ThresholdSelectionDescriptor<MessageComplianceThreshold> {

        @Override
        public String getDisplayName() {
            return "Message Compliance Threshold";
        }

        @Override
        public FormValidation doCheckValue(@QueryParameter String value, @QueryParameter boolean improvement) {
            if (!improvement) {
                try {
                    Integer parsedValue = Integer.parseInt(value);
                    if (parsedValue < 0) {
                        return FormValidation.error(Messages.PRQANotifier_WrongInteger());
                    }
                } catch (NumberFormatException ex) {
                    return FormValidation.error(Messages.PRQANotifier_UseNoDecimals());
                }
            }
            return FormValidation.ok();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/prqa-plugin/config/help-thresholds-message.html";
        }
    }
}
