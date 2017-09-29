package net.praqma.jenkins.plugin.prqa.utils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PRQABuildUtils {

    public static String normalizeWithEnv(final String in,
                                          final AbstractBuild<?, ?> build,
                                          final TaskListener listener) {

        FilePath buildWorkspace = build.getWorkspace();
        if (buildWorkspace == null) {
            throw new RuntimeException("Invalid workspace");
        }

        Boolean isOsWindows;
        try {
            isOsWindows = buildWorkspace.act(new MasterToSlaveCallable<Boolean, RuntimeException>() {
                @Override
                public Boolean call()
                        throws RuntimeException {
                    return SystemUtils.IS_OS_WINDOWS;
                }
            });
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            return in;
        }

        String out = in;

        if (StringUtils.isEmpty(out)) {
            return out;
        }

        Map<String, String> envVars = new TreeMap<>();

        try {
            EnvVars environment = build.getEnvironment(listener);

            envVars.putAll(environment);
        } catch (IOException | InterruptedException e) {
            // ignore
        }

        List<String> regexTemplate = new ArrayList<>();

        if (isOsWindows) {
            regexTemplate.add("%%%s%%");
        } else {
            regexTemplate.add("\\$\\{%s}");
            regexTemplate.add("\\$%s");
        }

        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            for (String regex : regexTemplate) {
                out = out.replaceAll(String.format(regex, entry.getKey()), entry.getValue());
            }
        }

        return out;
    }
}
