/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.plugin.prqa.globalconfig;

import java.io.Serializable;
import java.util.Objects;

import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Praqma
 */
public class QAVerifyServerConfiguration extends ToolInstallation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configurationName = "Configuration name";
    private String hostName;
    private Integer portNumber;
    private String userName;
    private String password;
    private String protocol;
    private Integer viewerPortNumber = 8080;
    private String externalUrl;

    @DataBoundConstructor
    public QAVerifyServerConfiguration(String configurationName,
                                       String hostName, Integer portNumber, String userName,
                                       String password, String protocol, Integer viewerPortNumber, String externalUrl) {
        super(configurationName, hostName, null);
        this.configurationName = configurationName;
        this.hostName = hostName;
        this.password = password;
        this.userName = userName;
        this.portNumber = portNumber;
        this.protocol = protocol;
        this.viewerPortNumber = viewerPortNumber;
        this.externalUrl = externalUrl;
    }

    /**
     * @return the configurationName
     */
    public String getConfigurationName() {
        return configurationName;
    }

    /**
     * @param configurationName the configurationName to set
     */
    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @return the portNumber
     */
    public Integer getPortNumber() {
        return portNumber;
    }

    /**
     * @param portNumber the portNumber to set
     */
    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return configurationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QAVerifyServerConfiguration that = (QAVerifyServerConfiguration) o;
        return Objects.equals(getConfigurationName(), that.getConfigurationName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConfigurationName());
    }

    public String getFullUrl() {
        String full = protocol + "://" + hostName + ":" + viewerPortNumber;
        return full;
    }

    public String getFullExternalUrl() {
        if (StringUtils.isEmpty(externalUrl)) {
            return getFullUrl();
        }
        return externalUrl;
    }


    /**
     * @return the viewerPortNumber
     */
    public Integer getViewerPortNumber() {
        return viewerPortNumber;
    }

    /**
     * @param viewerPortNumber the viewerPortNumber to set
     */
    public void setViewerPortNumber(Integer viewerPortNumber) {
        this.viewerPortNumber = viewerPortNumber;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }



}
