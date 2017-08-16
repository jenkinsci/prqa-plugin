package net.praqma.jenkins.plugin.prqa.utils;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PRQABuildUtils {

    public static String normalizeWithEnv(final String in, final Run<?, ?> build, final TaskListener listener) {

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

        if (SystemUtils.IS_OS_WINDOWS) {
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
