package net.praqma.prqa.execute;

import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CmdResult;
import net.praqma.util.execute.CommandLineInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class PrqaCommandLine
        implements CommandLineInterface {
    protected Logger logger = Logger.getLogger(PrqaCommandLine.class.getName());
    private static PrqaCommandLine instance = new PrqaCommandLine();
    private OperatingSystem thisos;
    private String[] cmd;
    private int last;

    private PrqaCommandLine() {
        this.thisos = OperatingSystem.WINDOWS;
        this.cmd = null;
        this.last = 0;
        String os = System.getProperty("os.name");
        this.logger.finer("Running on " + os);
        if (os.matches("^.*(?i)windows.*$")) {
            this.logger.finer("Using special windows environment");
            this.cmd = new String[3];
            this.cmd[0] = "cmd.exe";
            this.cmd[1] = "/C";
            this.last = 2;
        } else {
            this.thisos = OperatingSystem.UNIX;
            this.cmd = new String[3];
            this.cmd[0] = "bash";
            this.cmd[1] = "-c";
            this.last = 2;
        }

    }

    public OperatingSystem getOS() {
        return this.thisos;
    }

    public static PrqaCommandLine getInstance() {
        return instance;
    }

    public CmdResult run(String cmd)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, null, true, false);
    }

    public CmdResult run(String cmd,
                         File dir)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, dir, true, false);
    }

    public CmdResult run(String cmd,
                         File dir,
                         boolean merge)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, dir, merge, false);
    }

    public CmdResult run(String cmd,
                         File dir,
                         boolean merge,
                         boolean ignore)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, dir, merge, ignore, new HashMap<String, String>());
    }

    public synchronized CmdResult run(String cmd,
                                      File dir,
                                      boolean merge,
                                      boolean ignore,
                                      Map<String, String> variables)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, dir, merge, ignore, null, null);
    }

    public CmdResult run(String cmd,
                         File dir,
                         boolean merge,
                         boolean ignore,
                         PrintStream printStream)
            throws CommandLineException, AbnormalProcessTerminationException {
        return this.run(cmd, dir, merge, ignore, null, printStream);
    }

    public synchronized CmdResult run(String cmd,
                                      File dir,
                                      boolean merge,
                                      boolean ignore,
                                      Map<String, String> variables,
                                      PrintStream printStream)
            throws CommandLineException, AbnormalProcessTerminationException {
        this.cmd[this.last] = cmd;
        this.logger.config("$ " + cmd);

        try {
            ProcessBuilder pb = new ProcessBuilder(this.cmd);
            pb.redirectErrorStream(merge);
            if (dir != null) {
                this.logger.config("Executing command in " + dir);
                pb.directory(dir);
            }

            if (variables != null && variables.size() > 0) {
                this.logger.fine("CommandLine: " + variables);
                Map<String, String> env = pb.environment();
                Set<String> keys = variables.keySet();

                for (String key : keys) {
                    env.put(key, variables.get(key));
                }
            }

            CmdResult result = new CmdResult();
            Process p = pb.start();
            int exitValue = 0;
            StreamGobbler output;
            StreamGobbler errors;

            try (InputStream inputStream = p.getInputStream();
                 InputStream errorStream = p.getErrorStream()) {

                output = new StreamGobbler(inputStream, printStream);
                errors = new StreamGobbler(errorStream, printStream);

                output.start();
                errors.start();

                try {
                    exitValue = p.waitFor();
                } catch (InterruptedException var21) {
                    p.destroy();
                } finally {
                    Thread.interrupted();
                }

                try {
                    output.join();
                } catch (InterruptedException var20) {
                    this.logger.severe("Could not join output thread");
                }

                try {
                    errors.join();
                } catch (InterruptedException var19) {
                    this.logger.severe("Could not join errors thread");
                }
            }

            if (exitValue != 0) {
                this.logger.fine("Abnormal process termination(" + exitValue + "): " + errors.sres.toString());
                if (!ignore) {
                    if (merge) {
                        throw new AbnormalProcessTerminationException(output.sres.toString(), cmd, exitValue);
                    }

                    throw new AbnormalProcessTerminationException(errors.sres.toString(), cmd, exitValue);
                }
            }

            result.stdoutBuffer = output.sres;
            result.stdoutList = output.lres;
            result.errorBuffer = errors.sres;
            result.errorList = errors.lres;

            return result;
        } catch (IOException var23) {
            this.logger.warning("Could not execute the command \"" + cmd + "\" correctly: " + var23.getMessage());
            throw new CommandLineException(
                    "Could not execute the command \"" + cmd + "\" correctly: " + var23.getMessage());
        }
    }
}
