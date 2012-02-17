package net.praqma.jenkins.plugin.prqa.parser;

import net.praqma.jenkins.plugin.prqa.PrqaException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 *
 * @author jes
 */
public class HtmlParser {

    public static List<String> parse(String path, Pattern pattern) throws PrqaException {

        List<String> result = new ArrayList<String>();
        File file = new File(path);
        FileInputStream fis;

        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            throw new PrqaException("Could not find file " + file.getPath());
        }

        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader source = new BufferedReader(isr);
        String sourceLine = "";
        String report = "";
        try {
            while ((sourceLine = source.readLine()) != null) {
                report += sourceLine+"\n";
            }
        } catch (IOException ex) {
            throw new PrqaException("Could not read the line after :\n" + sourceLine);
        }
        Matcher match = pattern.matcher(report);
        while (match.find()) {
            result.add(match.group());
            System.out.println(match.group());
        }


        return result;
    }
}
