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
package net.praqma.jenkins.plugin.prqa.setup;

import hudson.EnvVars;
import hudson.Extension;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ListBoxModel;
import java.util.HashMap;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class QACToolSuite extends ToolInstallation implements PRQAToolSuite {
    
    public final String qarHome;
    public final String qawHome;
    public final String qavHome;
    public final String tool;
        
    @DataBoundConstructor
    public QACToolSuite(final String name, final String home, final String qarHome, final String qawHome, final String qavHome, final String tool) {
        super(name, home);
        this.qarHome = qarHome;
        this.qawHome = qawHome;
        this.qavHome = qavHome;
        this.tool = tool;
    }
    
    @Override
    public HashMap<String, String> createEnvironmentVariables(String workspaceRoot) {
        HashMap<String,String> environment =  null;
        if(tool.equals("qac")) {
            environment = new HashMap<String, String>();
            environment.put("QACPATH", getHome());
            environment.put("QACOUTPATH", workspaceRoot); //This one MUST be our workspace 
        } else if(tool.equals("qacpp")) {
            environment = new HashMap<String, String>();
            environment.put("QACPPPATH", getHome());
            environment.put("QACPPOUTPATH", workspaceRoot); //This one MUST be our workspace
        } 
        return environment;
    }        
    
    public HashMap<String,String> convert(EnvVars vars) {
        HashMap<String, String> varsMap = new HashMap<String, String>();
        for(String s:  vars.keySet()) {
            varsMap.put(s, vars.get(s));
        }
        return varsMap;
    }
    
    public static QACToolSuite getInstallationByName(String name) {
        if(StringUtils.isBlank(name)) {
            return null;
        } else {
            QACToolSuite[] installations = Jenkins.getInstance().getDescriptorByType(QACToolSuite.DescriptorImpl.class).getInstallations();
            for(QACToolSuite install : installations) {
                if(install.getName().equals(name)) {
                    return install;
                }
            }
        }
        return null;
    } 
    
    public static QACToolSuite[] getInstallations() {
        QACToolSuite[] installations = Jenkins.getInstance().getDescriptorByType(QACToolSuite.DescriptorImpl.class).getInstallations();
        return installations;
    }
        
    @Extension
    public static final class DescriptorImpl extends ToolDescriptor<QACToolSuite>  {

        public DescriptorImpl() {
            super();
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "PRQA Tool";
        }

        @Override
        public QACToolSuite newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            QACToolSuite suite = req.bindJSON(QACToolSuite.class, formData);
            
            save();
            return suite;        
            //return super.newInstance(req, formData);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }
        
        public ListBoxModel doFillToolItems () {
            ListBoxModel model = new ListBoxModel();            
            model.add("QA·C","qac");
            model.add("QA·C++","qacpp");
            return model;
        }
        //TODO: Implement this in a proper way. This one returns a warning when config page is reloaded.
        /*
        public FormValidation doCheckName(@QueryParameter String value) {            
            QACToolSuite suites = getInstallationByName(value);
            
            if(suites != null) {
                return FormValidation.errorWithMarkup("<p>A configuration with this name already exists. Ignore this message if it appears on load</p>");
            }
            return FormValidation.ok();
        }
        */ 
    }    
}
