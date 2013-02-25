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
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class PRQAToolSuite extends ToolInstallation {
    
    public final String qacHome;
    public final String qacppHome;
    public final String qarHome;    
    public final String qawHome;
    public final String installationName;
    public final String qavHome;

    
    @DataBoundConstructor
    public PRQAToolSuite(final String qacHome, final String qacppHome, final String qarHome,final String qawHome, final String installationName, final String qavHome) {        
        super(installationName, qacHome);
        this.installationName = installationName;
        this.qacHome = qacHome;
        this.qacppHome = qacppHome;
        this.qarHome = qarHome;
        this.qawHome = qawHome;
        this.qavHome = qavHome;
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        super.buildEnvVars(env);
        env.putAll(expandPrqaRequiredEnvironmentFiles(qacHome, qacppHome));
    }
    
    @Extension
    public static final class DescriptorImpl extends ToolDescriptor<PRQAToolSuite>  {

        @Override
        public ToolInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            PRQAToolSuite toolInstall = req.bindJSON(PRQAToolSuite.class, formData);
            save();
            return toolInstall;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "PRQA Tools";
        }

    }
    
    public Map<String,String> expandPrqaRequiredEnvironmentFiles(String qacHome, String qacppHome) {
        HashMap<String, String> environmentVars = new HashMap<String, String>();       
        if(qacHome != null ) {
            environmentVars.put("QACBIN", qacHome + "bin");
            environmentVars.put("QACHELPFILES", qacHome + "help");
            environmentVars.put("QACOUTPATH", qacHome + "temp");           
            environmentVars.put("QACPATH", qacHome);
            environmentVars.put("QACTEMP", qacHome + "temp");
        }
        
        if(qacppHome != null) {
            environmentVars.put("QACPPBIN", qacppHome + "bin");
            environmentVars.put("QACPPPATH", qacppHome);
            environmentVars.put("QACPPTEMP", qacppHome + "temp");
        }
        
        return environmentVars;
    }
    
    public String expandPrqaPathVariable(String qarHome, String qawHome) {
        return "";
    }
    
    public static PRQAToolSuite getInstallationByName(String name) {        
        PRQAToolSuite[] installations = Jenkins.getInstance().getDescriptorByType(PRQAToolSuite.DescriptorImpl.class).getInstallations();
        for(PRQAToolSuite install : installations) {
            if(install.getName().equals(name)) {
                return install;
            }
        }
        return null;
    }
}
