package net.praqma.jenkins.plugin.prqa;

import jenkins.model.Jenkins;

/**
 * @author jes
 */

public class VersionInfo {
    public static String getPluginVersion() {
        return String.format("Programming Research Quality Assurance Plugin version %s", Jenkins.getInstance().getPlugin("prqa-plugin").getWrapper().getVersion());        
    }
}