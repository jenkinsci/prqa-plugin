package net.praqma.prqa.execute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class StreamGobbler
        extends Thread {
    protected static Logger logger = Logger.getLogger(StreamGobbler.class.getName());
    public static final String linesep = System.getProperty("line.separator");

    InputStream is;
    PrintStream printStream;

    public StringBuffer sres;
    public List<String> lres;

    StreamGobbler(InputStream is) {
        this.is = is;
        lres = new ArrayList<>();
        sres = new StringBuffer();
    }

    StreamGobbler(InputStream is,
                  PrintStream printStream) {
        this.is = is;
        this.printStream = printStream;
        lres = new ArrayList<>();
        sres = new StringBuffer();
    }

    public StringBuffer getResultBuffer() {
        return sres;
    }

    public List<String> getResultList() {
        return lres;
    }

    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                // logger.info(line);
                lres.add(line);
                if (printStream != null) {
                    printStream.println(" > ".concat(line));
                }
            }

			/* Building buffer */
            for (int i = 0; i < lres.size() - 1; ++i) {
                sres.append(lres.get(i) + linesep);
            }

            if (lres.size() > 0) {
                sres.append(lres.get(lres.size() - 1));
            }

            synchronized (this) {
                notifyAll();
            }
        } catch (IOException ioe) {
            logger.severe("Could not read line from input stream.");
            ioe.printStackTrace();
        }
    }
}
