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
import hudson.model.Descriptor;

import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Praqma
 */
@Extension
public class PRQAGlobalConfig extends GlobalConfiguration {


    private List<QAVerifyServerConfiguration> servers = new ArrayList<>();

    @Inject
    public PRQAGlobalConfig() {
        load();
    }

    public PRQAGlobalConfig(List<QAVerifyServerConfiguration> servers) {
        this.servers = servers;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        boolean clean_servers = json.get("servers") == null && servers.size() > 0;
        if (clean_servers) {
            json.put("servers", new JSONObject());
        }
        req.bindJSON(this, json);
        save();
        boolean result = super.configure(req, json);
        if (clean_servers) {
            servers.clear();
        }
        return result;
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
    @SuppressWarnings("unused")
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

    public Collection<QAVerifyServerConfiguration> getConfigurationsByNames(Collection<String> names) {

        if (names == null) {
            return new ArrayList<>();
        }

        Collection<QAVerifyServerConfiguration> configurations = new ArrayList<>(names.size());

        for(QAVerifyServerConfiguration conf : getServers()) {
            if (names.contains(conf.getConfigurationName())){
                configurations.add(conf);
            }
        }

        return configurations;
    }

    @SuppressWarnings("unused")
    public List<Descriptor> descriptors() {
        Jenkins instance = Jenkins.getInstance();
        assert instance != null;
        return Collections.singletonList(instance
                                                .getDescriptor(QAVerifyServerConfiguration.class));
    }

    public FormValidation doCheckExternalUrl(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)){
            return FormValidation.ok();
        }
        try {
            new URL(value);
        } catch (MalformedURLException e) {
           return FormValidation.error(e.getMessage());
        }
        return FormValidation.ok();
    }

}