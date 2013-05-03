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
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author mads
 */
public class MessageComplianceThreshold extends AbstractThreshold {

    public final Integer value;
    public final int thresholdLevel;

    @DataBoundConstructor
    public MessageComplianceThreshold(final Integer value, final int thresholdLevel, final Boolean improvement) {
        super(improvement);
        this.value = value;
        this.thresholdLevel = thresholdLevel;
        
    }

    @Override
    public Boolean validateImprovement(PRQAComplianceStatus lastStableValue, PRQAComplianceStatus currentValue) {
        if(lastStableValue == null)
            return Boolean.TRUE;
        return currentValue.getMessageCount(thresholdLevel) <= lastStableValue.getMessageCount(thresholdLevel);
    }

    @Override
    public String onUnstableMessage(PRQAComplianceStatus lastStableValue, PRQAComplianceStatus currentValue) {
        if(improvement) {
            return Messages.PRQANotifier_MaxMessagesRequirementNotMet(currentValue, lastStableValue);
        } else {
            return Messages.PRQANotifier_MaxMessagesRequirementNotMet(currentValue, value);
        }
    }

    @Override
    public Boolean validateThreshold(PRQAComplianceStatus currentValue) {
        return currentValue.getMessageCount(thresholdLevel) <= value;
    }
    
    @Override
    public Boolean validate(PRQAComplianceStatus lastStableValue, PRQAComplianceStatus currentValue) {
        currentValue.setMessagesWithinThreshold(thresholdLevel);
        if(improvement) {
            return validateImprovement(lastStableValue, currentValue);
        } else {
            return validateThreshold(currentValue);
        }
    }
    
    @Extension
    public static final class DescriptorImpl extends ThresholdSelectionDescriptor<MessageComplianceThreshold> {
        
        @Override
        public String getDisplayName() {
            return "Message Compliance Threshold";
        }
        
        public ListBoxModel doFillThresholdLevelItems() {
            ListBoxModel model = new ListBoxModel();
            for (int i = 0; i < 10; i++) {
                model.add("" + i);
            }
            return model;
        }
        
        public FormValidation doCheckValue(@QueryParameter String value) {
            try {
                Integer parsedValue = Integer.parseInt(value);
                if(parsedValue < 0) {
                    return FormValidation.error(Messages.PRQANotifier_WrongInteger());
                }
            } catch (NumberFormatException ex) {
                return FormValidation.error(Messages.PRQANotifier_UseNoDecimals());
            }
            return FormValidation.ok();
        }
        
        @Override
        public String getHelpFile() {
            return "/plugin/prqa-plugin/config/help-thresholds.html";
        }  
    }
}
