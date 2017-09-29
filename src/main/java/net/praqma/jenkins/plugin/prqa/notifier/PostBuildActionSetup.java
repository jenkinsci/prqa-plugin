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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Praqma
 */
public class PostBuildActionSetup
        implements Describable<PostBuildActionSetup>, ExtensionPoint, Serializable {

    @Override
    public Descriptor<PostBuildActionSetup> getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        }

        //noinspection unchecked
        return (Descriptor<PostBuildActionSetup>) instance.getDescriptorOrDie(getClass());
    }

    /**
     * PRQAReportSource
     * All registered {@link PostBuildActionSetup}s.
     */
    public static DescriptorExtensionList<PostBuildActionSetup, PRQAReportSourceDescriptor<PostBuildActionSetup>> all() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        }

        return instance.getDescriptorList(PostBuildActionSetup.class);
    }

    public static List<PRQAReportSourceDescriptor<?>> getDescriptors() {
        List<PRQAReportSourceDescriptor<?>> descriptors = new ArrayList<>();
        DescriptorExtensionList<PostBuildActionSetup, PRQAReportSourceDescriptor<PostBuildActionSetup>> all = all();
        if (all == null) {
            return descriptors;
        }

        descriptors.addAll(all);

        return descriptors;
    }

}
