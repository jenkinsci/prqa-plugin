/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.products;

import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.execute.PrqaCommandLine;
import net.praqma.prqa.reports.QAFrameworkReport;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CmdResult;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Alexandru Ion
 */
public class QACli
        implements Product, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(QACli.class.getName());
    public static final String QAF_BIN_PATH = "QAFBINPATH";
    public static final String WORKSPACE_PATH = "WORKSPACEPATH";
    public static final String QAF_INSTALL_PATH = "QAFINSTALLPATH";
    public static final String QACLI = "qacli";

    @Override
    public final String getProductVersion(Map<String, String> environment,
                                          File workspace,
                                          boolean isUnix)
            throws PrqaSetupException {
        logger.finest("Starting execution of method - getProductVersion()");

        String productVersion;
        CmdResult res;
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        if (environment.containsKey(QAF_BIN_PATH)) {
            sb.append(environment.get(QAF_BIN_PATH));
            sb.append(System.getProperty("file.separator"));
        }
        sb.append(QACLI)
          .append("\" --version");

        try {
            res = PrqaCommandLine.getInstance()
                                 .run(sb.toString(), workspace, true, false, environment);
            StringBuffer strBuffer = res.stdoutBuffer;
            productVersion = strBuffer.substring(strBuffer.indexOf(":") + 1, strBuffer.length())
                                      .trim();

        } catch (AbnormalProcessTerminationException abnex) {
            logger.warning(
                    String.format("Failed to detect QA·CLI version with command %s returned code %s%nMessage was:%n%s",
                                  abnex.getCommand(), abnex.getExitValue(), abnex.getMessage()));
            Map<String, String> systemVars = new HashMap<>();
            systemVars.putAll(System.getenv());

            systemVars.putAll(environment);

            QAFrameworkReport._logEnv("Error in QACLI.getProductVersion() - Printing environment", systemVars);
            throw new PrqaSetupException(String.format("Failed to detect QA·CLI version%n%s", abnex.getMessage()));

        }
        logger.finest(String.format("Returning value %s", productVersion));
        return productVersion;
    }
}
