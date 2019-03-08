package net.praqma.jenkins.plugin.prqa;

import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.io.Serializable;

/**
 * @author jes
 */

public class VersionInfo
        implements Serializable {

    public static final String WIKI_PAGE = "https://wiki.jenkins-ci.org/display/JENKINS/PRQA+Plugin";

    private static final String ARTIFACT_ID = "prqa-plugin";

    public static String getPluginVersion() {
        return Jenkins.get().getPlugin(ARTIFACT_ID).getWrapper().getVersion();
    }
}
