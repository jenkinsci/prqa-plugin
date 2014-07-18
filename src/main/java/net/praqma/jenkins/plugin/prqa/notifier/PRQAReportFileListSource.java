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

import hudson.Extension;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author Praqma
 */
public class PRQAReportFileListSource extends PRQAFileProjectSource {
    public final String fileList;
    public final String settingsFile;
    
    @DataBoundConstructor
    public PRQAReportFileListSource(String fileList, String settingsFile) {
        this.fileList = fileList;
        this.settingsFile = settingsFile;
    }
    
    @Extension
    public final static class DescriptorImpl extends PRQAFileProjectSourceDescriptor<PRQAReportFileListSource> {

        public DescriptorImpl() {
            super();
        }
        
        @Override
        public String getDisplayName() {
            return "File list";
        }
        
        public FormValidation doCheckFileList(@QueryParameter String value) {
            if(StringUtils.isBlank(value)) {
                return FormValidation.error("File list can not be empty");
            } else {
                return FormValidation.ok();
            }
        }
        
    }
}
