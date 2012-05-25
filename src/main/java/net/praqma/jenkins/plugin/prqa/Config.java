package net.praqma.jenkins.plugin.prqa;

import jenkins.model.Jenkins;

/**
 * @author jes
 */

public class Config {
    public static final String ICON_NAME="/plugin/prqa-plugin/images/32x32/prqa.png";
    public static final String PROJECT_WIKI = "https://wiki.jenkins-ci.org/display/JENKINS/PRQA+Plugin";
    
    public static final String PLUGIN_NAME = "prqa-plugin";
    
    public static String getPluginVersion() {
        return String.format("Programming Research Quality Assurance Plugin version %s", Jenkins.getInstance().getPlugin(Config.PLUGIN_NAME).getWrapper().getVersion());
    }
}