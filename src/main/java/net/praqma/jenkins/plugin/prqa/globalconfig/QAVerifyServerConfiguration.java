/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.plugin.prqa.globalconfig;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Praqma
 */
public class QAVerifyServerConfiguration {
    
    private String configurationName = "Configuration name";
    private String hostName = "http://myserver:8080";
    private Integer portNumber = 22230;
    
    private String userName = "upload";
    private String password = "upload";
    
    @DataBoundConstructor
    public QAVerifyServerConfiguration(String configurationName, String hostName, Integer portNumber, String userName, String password) {
        this.configurationName = configurationName;
        this.hostName = hostName;
        this.password = password;
        this.userName = userName;
        this.portNumber = portNumber;
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
    public boolean equals(Object obj) {
        if (obj instanceof QAVerifyServerConfiguration) {
            if(this == obj) {
                return true;
            }
            
            QAVerifyServerConfiguration qavsc = (QAVerifyServerConfiguration)obj;
            
            return (qavsc.getConfigurationName() != null && getConfigurationName() != null) && qavsc.getConfigurationName().equals(getConfigurationName()); 
            
            
        } else {
            return false;
        }
    }
    
    
    
}
