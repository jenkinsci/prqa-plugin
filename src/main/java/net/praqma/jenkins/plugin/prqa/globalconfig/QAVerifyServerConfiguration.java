/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.plugin.prqa.globalconfig;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import net.praqma.jenkins.plugin.prqa.notifier.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Praqma
 */
public class QAVerifyServerConfiguration
        extends AbstractDescribableImpl<QAVerifyServerConfiguration>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configurationName = "Configuration name";
    private String hostName;
    private Integer portNumber;
    private String userName;
    private String password;
    private String protocol;
    private Integer viewerPortNumber = 8080;

    @DataBoundConstructor
    public QAVerifyServerConfiguration(String configurationName,
                                       String hostName,
                                       Integer portNumber,
                                       String userName,
                                       String password,
                                       String protocol,
                                       Integer viewerPortNumber) {
        this.configurationName = configurationName;
        this.hostName = hostName;
        this.password = password;
        this.userName = userName;
        this.portNumber = portNumber;
        this.protocol = protocol;
        this.viewerPortNumber = viewerPortNumber;
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
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
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
        return Objects.equals(getConfigurationName(),
                              that.getConfigurationName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConfigurationName());
    }

    public String getFullUrl() {
        return protocol + "://" + hostName + ":" + viewerPortNumber;
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
    @DataBoundSetter
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
    @DataBoundSetter
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public enum ViewServerProtocol {
        http,
        https,
    }

    @Extension
    public static class DescriptorImpl
            extends Descriptor<QAVerifyServerConfiguration> {

        public DescriptorImpl() {
            super(QAVerifyServerConfiguration.class);
            load();
        }

        public ViewServerProtocol[] getViewServerProtocols() {
            return ViewServerProtocol.values();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "QA·Verify Server";
        }

        public FormValidation doCheckConfigurationName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckHostName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckPortNumber(@QueryParameter String value) {
            return checkValidPort(value);
        }

        public FormValidation doCheckUserName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckViewerPortNumber(@QueryParameter String value) {
            return checkValidPort(value);
        }

        private FormValidation checkValidPort(@QueryParameter String value) {
            Integer valueOf;
            try {
                valueOf = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                valueOf = -1;
            }

            if (valueOf <= 0 || valueOf > 0x0000FFFF) {
                return FormValidation.error(Messages.QAVerifyServerConfiguration_InvalidPort());
            }

            return FormValidation.ok();
        }
    }
}
