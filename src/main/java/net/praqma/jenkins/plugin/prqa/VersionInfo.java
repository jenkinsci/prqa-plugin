package net.praqma.jenkins.plugin.prqa;

import java.io.Serializable;
import jenkins.model.Jenkins;

/**
 * @author jes
 */

public class VersionInfo implements Serializable{

    public static final String WIKI_PAGE="https://wiki.jenkins-ci.org/display/JENKINS/PRQA+Plugin";

    private static final String ARTIFACT_ID = "prqa-plugin";

    public static String getPluginVersion() {
        Jenkins jenkins = Jenkins.getInstance();

        if (jenkins == null) {
            throw new RuntimeException("Unable to get Jenkins instance");
        }

        return jenkins.getPlugin(ARTIFACT_ID).getWrapper().getVersion();
    }
}