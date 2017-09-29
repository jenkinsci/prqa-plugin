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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.praqma.jenkins.plugin.prqa.notifier.ThresholdSelectionDescriptor;
import net.praqma.prqa.status.PRQAComplianceStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mads
 */
public abstract class AbstractThreshold
        implements Describable<AbstractThreshold>,
                   ExtensionPoint,
                   Serializable {

    public final Boolean improvement;

    AbstractThreshold(final Boolean improvement) {
        this.improvement = improvement;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<AbstractThreshold> getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        }

        return (Descriptor<AbstractThreshold>) instance.getDescriptorOrDie(getClass());
    }

    public static DescriptorExtensionList<AbstractThreshold, ThresholdSelectionDescriptor<AbstractThreshold>> all() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        }

        return instance.getDescriptorList(AbstractThreshold.class);
    }

    public static List<ThresholdSelectionDescriptor<?>> getDescriptors() {
        List<ThresholdSelectionDescriptor<?>> list = new ArrayList<>();
        DescriptorExtensionList<AbstractThreshold, ThresholdSelectionDescriptor<AbstractThreshold>> all = all();
        if (all != null) {
            list.addAll(all);
        }

        return list;
    }

    public abstract boolean validateImprovement(PRQAComplianceStatus previousComplianceStatus,
                                                PRQAComplianceStatus currentComplianceStatus);

    public abstract boolean validateThreshold(PRQAComplianceStatus currentComplianceStatus);

    public boolean validate(PRQAComplianceStatus previousComplianceStatus,
                            PRQAComplianceStatus currentComplianceStatus) {
        if (improvement) {
            return validateImprovement(previousComplianceStatus,
                                       currentComplianceStatus);
        } else {
            return validateThreshold(currentComplianceStatus);
        }
    }
}
