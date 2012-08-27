package net.praqma.jenkins.plugin.prqa.globalconfig;

/*
 * The MIT License
 *
 * Copyright 2012 Praqma.
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


import hudson.Extension;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
@Extension
public class PRQAGlobalConfig extends GlobalConfiguration {
    
    public enum ViewServerProtocol {
        http,
        https,
    }
    
    private List<QAVerifyServerConfiguration> servers = new ArrayList<QAVerifyServerConfiguration>();
    
    public PRQAGlobalConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        //return super.configure(req, json);
        return true;
    }
    
    public static PRQAGlobalConfig get() {
        return GlobalConfiguration.all().get(PRQAGlobalConfig.class);
    }

    /**
     * @return the servers
     */
    public List<QAVerifyServerConfiguration> getServers() {
        return servers;
    }

    /**
     * @param servers the servers to set
     */
    public void setServers(List<QAVerifyServerConfiguration> servers) {
        this.servers = servers;
    }
    
    public QAVerifyServerConfiguration getConfigurationByName(String name) {
        for(QAVerifyServerConfiguration conf : getServers()) {
            if(conf.getConfigurationName().equals(name)) {
                return conf;
            }
        }
        return null;
    }
    
    public ViewServerProtocol[] getViewServerProtocols() {
        return ViewServerProtocol.values();  
    }
    
    //Disabled this for now. I have no idea as to why this makes the global confiuration unloadable.
    /*
    public FormValidation doCheckConfigurationName(@QueryParameter String value) {
        if(getConfigurationByName(value) != null) {
            return FormValidation.error("A configuration with that name already exists");
        } else {
            if(StringUtils.isBlank(value)) {
                return FormValidation.error("Configuration name must be set");
            }
            return FormValidation.ok();
        }
    }
    */
    
    /*
    public FormValidation doCheckHostName(@QueryParameter String value) {
        if(StringUtils.isBlank(value)) {
            return FormValidation.error("Hostname must not be empty");
        }
        return FormValidation.ok();
    }
    */

    

}
