package net.praqma.jenkins.plugin.prqa.parser;

import java.io.*;
import net.praqma.jenkins.plugin.prqa.PrqaException;
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
                report += sourceLine + "\n";
            }
        } catch (IOException ex) {
            throw new PrqaException("Could not read the line after :\n" + sourceLine);
        }
        Matcher match = pattern.matcher(report);
       while (match.find()) {
            result.add(match.group(1));
            System.out.println(match.group());
        }


        return result;
    }
}
