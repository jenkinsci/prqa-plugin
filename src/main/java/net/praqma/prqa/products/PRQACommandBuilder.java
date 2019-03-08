/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.products;

import net.praqma.prqa.CodeUploadSetting;
import net.praqma.prqa.exceptions.PrqaException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * @author Praqma
 */
public class PRQACommandBuilder
        implements Serializable {

    private String executable;
    private LinkedList<String> arguments = new LinkedList<>();
    private static final Logger logger;

    static {
        logger = Logger.getLogger(PRQACommandBuilder.class.getName());
    }

    public PRQACommandBuilder(String executable) {
        logger.finest(String.format("Constructor called for class PRQACommandBuilder()"));
        logger.finest(
                String.format("Input parameter executable type: %s; value: %s", executable.getClass(), executable));

        this.executable = executable;

        logger.finest(String.format("Ending execution of constructor - PRQACommandBuilder()"));
    }

    public PRQACommandBuilder appendArgument(String argument) {
        if (!StringUtils.isBlank(argument)) {
            logger.finest(String.format("Starting execution of method - appendArgument"));
            logger.finest(String.format("Input parameter argument type: %s; value: %s", argument.getClass(), argument));
            arguments.addLast(argument);
        }

        logger.finest(String.format("Returning %s", this));

        return this;
    }

    public String getCommand() {
        StringBuilder builder = new StringBuilder();
        logger.finest(String.format("Starting execution of method - getCommand"));

        builder.append(executable)
               .append(" ");

        for (String s : arguments) {
            builder.append(s)
                   .append(" ");

        }

        logger.finest(String.format("Returning value: %s", builder.toString()));

        return builder.toString();
    }

    /**
     * Resolves the project file location. This can be either absolute or
     * relative to the current workspace
     * @param workspaceRoot workspace root
     * @param filePath file path
     * @return project file
     * @throws PrqaException when thrown
     */
    public static String resolveAbsOrRelativePath(File workspaceRoot,
                                                  String filePath)
            throws PrqaException {
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(workspaceRoot, filePath);
        }
        if (!file.exists()) {
            throw new PrqaException(String.format("The file %s does not exist.", file.getPath()));
        }
        return file.getPath();
    }

    public static String wrapFile(File workspaceRoot,
                                  String filePath)
            throws PrqaException {
        return wrapInQuotationMarks(resolveAbsOrRelativePath(workspaceRoot, filePath));
    }

    public static String getCmaf(String path,
                                 boolean escapeInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getCmaf(String path, boolean escapeInputParameterWhiteSpace)"));
        logger.finest(String.format("Input parameter path type: %s; value: %s", path.getClass(), path));
        logger.finest(String.format("Input parameter escapeInputParameterWhiteSpace type: %s; value: %s", "boolean",
                                    escapeInputParameterWhiteSpace));

        if (escapeInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format("-cmaf \"%s\"", path.replace(" ", "\\ "));

            logger.finest(String.format("Returning value: %s", output));

            return output;
        }

        String output = String.format("-cmaf \"%s\"", path);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getMaseq(String commandSequence,
                                  boolean escapeInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getMaseq(String commandSequence, boolean escapeInputParameterWhiteSpace)"));
        logger.finest(String.format("Input parameter commandSequence type: %s; value: %s", commandSequence.getClass(),
                                    commandSequence));
        logger.finest(String.format("Input parameter escapeInputParameterWhiteSpace type: %s; value: %s", "boolean",
                                    escapeInputParameterWhiteSpace));

        if (escapeInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format(commandSequence.replace(" ", "\\ "), commandSequence);

            logger.finest(String.format("Returning value: %s", output));


            return String.format("-maseq \"%s\"", output);
        }

        String output = String.format("-maseq \"%s\"", commandSequence);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getReportFormatParameter(String reportFormat,
                                                  boolean escapeinInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getReportFormatParameter(String reportFormat, boolean escapeinInputParameterWhiteSpace)"));

        if (escapeinInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format("-po qar::report_format=%s", reportFormat.replace(" ", "\\ "));

            logger.finest(String.format("Returning value: %s", output));

            return output;
        }

        String output = String.format("-po qar::report_format=%s", reportFormat);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getReportTypeParameter(String reportType,
                                                boolean escapeInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getReportTypeParameter(String reportType, boolean escapeInputParameterWhiteSpace)"));

        if (escapeInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format("-po qar::report_type=%s\\ Report", reportType.replace(" ", "\\ "));

            logger.finest(String.format("Returning value: %s", output));

            return output;
        }

        String output = String.format("-po qar::report_type=%s\\ Report", reportType);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getOutputPathParameter(String outpath,
                                                boolean escapeInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getOutputPathParameter(String outpath, boolean escapeInputParameterWhiteSpace)"));


        if (escapeInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format("-po qar::output_path=%s", outpath.replace(" ", "\\ "));

            logger.finest(String.format("Returning value: %s", output));

            return output;
        }

        String output = String.format("-po qar::output_path=%s", outpath);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getProjectName() {
        logger.finest(String.format("Starting execution of method - getProjectName"));

        String output = "-po qar::project_name=%J";

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getViewingProgram(String program,
                                           boolean escapeInputParameterWhiteSpace) {
        logger.finest(String.format(
                "Starting execution of method - getViewingProgram(String program, boolean escapeInputParameterWhiteSpace)"));

        if (escapeInputParameterWhiteSpace) {
            logger.finest(String.format("Replacing spaces with \"\\ \""));

            String output = String.format("-po qar::viewing_program=%s", program.replace(" ", "\\ "));

            logger.finest(String.format("Returning value: %s", output));

            return output;
        }

        String output = String.format("-po qar::viewing_program=%s", program);

        logger.finest(String.format("Returning value: %s", output));

        return output;
    }

    public static String getHost(String hostname) {
        logger.entering(PRQACommandBuilder.class.getName(), "getHost", hostname);
        String host = String.format("-host %s", hostname);
        logger.exiting(PRQACommandBuilder.class.getName(), "getHost", host);
        return host;
    }

    public static String getUser(String user) {
        logger.entering(PRQACommandBuilder.class.getName(), "getUser", user);
        String userres = String.format("-user %s", user);
        logger.exiting(PRQACommandBuilder.class.getName(), "getUser", userres);
        return userres;
    }

    public static String getPassword(String password) {
        logger.entering(PRQACommandBuilder.class.getName(), "getPassword", password);

        String pass;
        if (StringUtils.isBlank(password)) {
            pass = "";
        } else {
            pass = String.format("-pass %s", password);
        }
        logger.exiting(PRQACommandBuilder.class.getName(), "getPassword", pass);
        return pass;
    }

    public static String getProjectDatabase(String databaseName) {
        logger.entering(PRQACommandBuilder.class.getName(), "getProjectDatabase", databaseName);
        String dbname = String.format("-db %s", databaseName);
        logger.exiting(PRQACommandBuilder.class.getName(), "getProjectDatabase", dbname);
        return dbname;
    }

    public static String getSingle(boolean useSingleSnapshotMode) {
        logger.entering(PRQACommandBuilder.class.getName(), "getSingle", useSingleSnapshotMode);
        if (useSingleSnapshotMode) {
            logger.exiting(PRQACommandBuilder.class.getName(), "getSingle", "-single");
            return "-single";
        }
        logger.exiting(PRQACommandBuilder.class.getName(), "getSingle", "");
        return "";
    }

    public static String getCodeAll(CodeUploadSetting setting) {
        switch (setting) {
            case AllCode:
                return "-po qav::code=all";
            case None:
                return "";
            case OnlyNew:
                return "-po qav::code";
            default:
                return "";
        }
    }

    public static String escapeWhitespace(String string) {
        return string.replace(" ", "\\ ");
    }

    public static String wrapInQuotationMarks(String string) {
        logger.entering(PRQACommandBuilder.class.getName(), "wrapInQuotationMarks", string);
        String wrapped = String.format("\"%s\"", string);
        logger.exiting(PRQACommandBuilder.class.getName(), "wrapInQuotationMarks", wrapped);
        return wrapped;
    }

    public static String getQavOutPathParameter(String outpath) {
        return getQavOutPathParameter(outpath, false);
    }

    public static String getQavOutPathParameter(String outpath,
                                                boolean escapeInputParameterWhiteSpace) {
        logger.entering(PRQACommandBuilder.class.getName(), "getQavOutPathParameter", escapeInputParameterWhiteSpace);
        String out = "";
        if (escapeInputParameterWhiteSpace) {
            out = String.format("-po qav::output=\\\"%s\\\"", outpath.replace(" ", "\\ "));
        } else {
            out = String.format("-po qav::output=\\\"%s\\\"", outpath);
        }
        logger.exiting(PRQACommandBuilder.class.getName(), "getQavOutPathParameter", out);
        return out;
    }

    public static String getVcsXmlString(String vcsXmlPath) {
        logger.entering(PRQACommandBuilder.class.getName(), "getVcsXmlString", new Object[]{vcsXmlPath});
        String vcsxml = String.format("-po qav::prqavcs=\\\"%s\\\"", vcsXmlPath);
        logger.exiting(PRQACommandBuilder.class.getName(), "getVcsXmlString", vcsxml);
        return vcsxml;
    }

    //RQ-6
    public static String getPrqaVcs(CodeUploadSetting setting,
                                    String vcsXmlPath) {
        String res = "";
        if (setting.equals(CodeUploadSetting.AllCode) && StringUtils.isBlank(vcsXmlPath)) {
            return res;
        } else {
            res = PRQACommandBuilder.getVcsXmlString(vcsXmlPath);
        }
        return res;
    }

    public static String getRepositorySetting(String repositoyPath) {
        String res = "";
        if (!StringUtils.isBlank(repositoyPath)) {
            res = String.format("-r %s", repositoyPath);
        }
        return res;
    }

    public static String getMessageConfigurationParameter(String projConfigXml) {
        String res = "";
        if (!StringUtils.isBlank(projConfigXml)) {
            res = String.format("-config %s", projConfigXml);
        }
        return res;
    }

    public static String getLogFilePathParameter(String fullLogFilePath) {
        return String.format("-log \\\"%s\\\"", fullLogFilePath);
    }

    public static String getImportLogFilePathParameter(String fullLogFilePath) {
        return String.format("-po qav::log=\\\"%s\\\"", fullLogFilePath);
    }

    /**
     * Method which creates the necessary sfba option. The boolean indicates whether the project has just been analyzed, if that is the case. The -sfba parameter is returned
     * @param wasAnalyzedBeforeHand project was analyzed beforehand
     * @return option
     */
    public static String getSfbaOption(boolean wasAnalyzedBeforeHand) {
        String res = "";
        if (wasAnalyzedBeforeHand) {
            res = "-sfba";
        }
        return res;
    }

    public static String getNumberOfThreads(int number) {
        return String.format("-po qav::thread=%s", number);
    }

    public static String getSop(String topLevelSourceDir) {
        String sourceDir = topLevelSourceDir;
        if (topLevelSourceDir.endsWith("\\")) {
            sourceDir = topLevelSourceDir.substring(0, sourceDir.length() - 1);
        }

        return String.format("-sop \\\"%s\\\"", sourceDir);
    }

    public static String getProd(boolean single) {
        String res = "";
        res += "-prod %Q ";
        if (single) {
            res += PRQACommandBuilder.getSingle(single) + " ";
        }
        return res;
    }

    public static String getPort(int port) {
        return String.format("-port %s", port);
    }

    public static String wrapInEscapedQuotationMarks(String text) {
        return String.format("\\\"%s\\\"", text);
    }

    public static String getDataFlowAnanlysisParameter(boolean enabled) {
        String res = "";
        if (enabled) {
            res = "-ed+";
        }
        return res;
    }

    public static String getDependencyModeParameter(boolean enabled) {
        String res = "";
        if (enabled) {
            res = "-mode depend";
        }
        return res;
    }

    public static String getCrossModuleAnalysisParameter(boolean enabled) {
        String res = "";
        if (enabled) {
            res = "pal %Q %P+ %L+#";
        }
        return res;
    }
}
