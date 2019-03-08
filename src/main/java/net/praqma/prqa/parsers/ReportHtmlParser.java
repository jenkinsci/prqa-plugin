/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.parsers;

import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Praqma
 */
public abstract class ReportHtmlParser
        implements Serializable {

    protected String fullReportPath;
    private static final Logger logger;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    static {
        logger = Logger.getLogger(ReportHtmlParser.class.getName());
    }

    public ReportHtmlParser() {

    }

    public ReportHtmlParser(String fullReportPath) {
        this.fullReportPath = fullReportPath;
    }

    /**
     * Parse method. Takes a path to a file, and a pattern for which to scan for.
     *
     * @return full report path
     */
    public String getFullReportPath() {
        logger.finest(String.format("Returning value: %s", this.fullReportPath));
        return this.fullReportPath;
    }

    public void setFullReportPath(String fullReportPath) {
        logger.finest("Starting execution of method - setFullReportPath");
        this.fullReportPath = fullReportPath;
        logger.finest("Ending execution of method - setFullReportPath");
    }

    public String getParseFirstResult(Pattern pattern)
            throws PrqaException {
        logger.finest("Starting execution of method - getResult");
        String output = getFirstResult(parse(this.fullReportPath, pattern));
        logger.finest(String.format("Returning value: %s", output));
        return output;
    }

    public List<String> getParseResults(Pattern pattern)
            throws PrqaException {
        logger.finest("Starting execution of method - getResult");
        return parse(this.fullReportPath, pattern);
    }

    public List<String> parse(String path,
                              Pattern pattern)
            throws PrqaParserException {
        logger.finest("Starting execution of method - parse");

        List<String> result = new ArrayList<>();
        File file = new File(path);
        FileInputStream fis;

        logger.finest("Attempting to open filepath: " + file.getAbsolutePath());
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            logger.severe(String.format("Exception thrown type: %s; message: %s", ex.getClass(), ex.getMessage()));
            throw new PrqaParserException(ex);
        }
        logger.finest("File opened successfully!");

        try (InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader source = new BufferedReader(isr)) {

            String sourceLine = "";
            Matcher match;
            String report = "";

            logger.finest("Attempting to read the file...");

            while ((sourceLine = source.readLine()) != null) {
                report += sourceLine + ReportHtmlParser.LINE_SEPARATOR;
                match = pattern.matcher(report);

                if (match.find()) {
                    logger.finest("Match found!");

                    result.add(match.group(1));

                    logger.finest("Returning result:");
                    for (String s : result) {
                        logger.finest(String.format("    %s", s));
                    }
                    return result;
                }
            }
        } catch (IOException ex) {
            logger.severe(String.format("Exception thrown type: %s; message: %s", ex.getClass(), ex.getMessage()));
            throw new PrqaParserException(ex);
        }

        logger.finest("File read successfully!");

        logger.finest("Returning result:");
        for (String s : result) {
            logger.finest(String.format("    %s", s));
        }

        return result;
    }

    public String getFirstResult(List<String> results) {
        logger.finest("Starting execution of method - getFirstResult");
        if (results.size() > 0) {
            String output = results.get(0);
            logger.finest(String.format("Returning value: %s", output));
            return output;
        }

        logger.finest("Collection is empty, returning null.");
        return null;
    }
}
