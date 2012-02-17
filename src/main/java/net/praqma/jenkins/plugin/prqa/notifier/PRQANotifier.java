/**
 *
 * @author jes
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.Extension;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.plugin.prqa.PrqaException;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.praqma.jenkins.plugin.prqa.Config;
import net.praqma.jenkins.plugin.prqa.parser.HtmlParser;
import net.praqma.prqa.PRQAContext.AnalyseTypes;
import net.praqma.prqa.PRQAContext.QARReportType;
import net.praqma.prqa.products.QAC;
import net.praqma.prqa.products.QACpp;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class PRQANotifier extends Publisher {

    private PrintStream out;
    private Status status;
    private Boolean totalBetter;
    private Integer totalMax;
    private String prodct;
    private QARReportType reportType;
    private String command;
    private String prqaHome;

    @DataBoundConstructor
    public PRQANotifier(String command, String reportType, String product,
            boolean totalBetter, String totalMax) {
        this.reportType = QARReportType.valueOf(reportType);
        this.prodct = product;
        this.totalBetter = totalBetter;
        this.totalMax = totalMax == null || totalMax.equals("") ? null : Integer.parseInt(totalMax);
        this.command = command;
        //this.prqaHome = ;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        //TODO copied from CCUCM NOTIFIER ask chw why this is like this????
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        boolean result = false;

        out = listener.getLogger();

        executed();
//        out.println("Validating if build better than last: " + totalBetter);
//        out.println("Report Type: " + reportType);
//        out.println("Product: " + prodct);
//        out.println("QAC / QACpp command: " + command);
//        //out.println("PRQA_HOME: " + prqaHome );


        if (totalMax != null && (totalBetter)) {
            out.println("You cannot check both for max number of msg, and if the build was better than last. pick one!");
            return result;
        }

        /**
         * get total count of messages
         *  and set status
         */
        status = new Status();
        Pattern pattern = Pattern.compile("<td align=\"left\">Total Number of Messages</td>\n<td align=\"right\">(.?)<");

        try {
            out.println("File path: " + Config.COMPLIANCE_REPORT_PATH);
            out.println(pattern);

            out.println(HtmlParser.parse(Config.COMPLIANCE_REPORT_PATH, pattern).get(0));
        } catch (PrqaException ex) {
            Logger.getLogger(PRQANotifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        status.setMessages(Config.resultMessages);


        //Validates if its better than last job
        if (totalBetter) {
            //Gettting results from the previus build
            AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
            //a list of publisher from prev build
            DescribableList<Publisher, Descriptor<Publisher>> publishers = prevBuild.getProject().getPublishersList();

            PRQANotifier publisher = (PRQANotifier) publishers.get(PRQANotifier.class);
            if (publisher == null) {
                throw new IOException();
            }

            if (publisher.getStatus().getMessages() >= status.getMessages()) {
                result = true;
            }

        } //validates if its below the treshold
        else if (totalMax != null) {

            //validate
            if (totalMax >= status.getMessages()) {
                result = true;
            }
        }
        return result;
    }

    private void executed() {

        switch (AnalyseTypes.valueOf(prodct)) {
            case QAC:
                new QAC().execute(command);
                break;
            case QACpp:
                new QACpp().execute(command);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Exported
    public String getPrqaHome() {
        return prqaHome;
    }

    @Exported
    public void setPrqaHome(String prqaHome) {
        this.prqaHome = prqaHome;
    }

    @Exported
    public String getCommand() {
        return command;
    }

    @Exported
    public Integer getTotalMax() {
        return this.totalMax;
    }

    @Exported
    public void setTotalMax(Integer totalMax) {
        this.totalMax = totalMax;
    }

    @Exported
    public void setCommand(String Command) {
        this.command = Command;
    }

    @Exported
    public String getProdct() {
        return prodct;
    }

    @Exported
    public void setProdct(String prodct) {
        this.prodct = prodct;
    }

    @Exported
    public QARReportType getReportType() {
        return reportType;
    }

    @Exported
    public void setReportType(QARReportType reportType) {
        this.reportType = reportType;
    }

    @Exported
    public Boolean getTotalBetter() {
        return totalBetter;
    }

    @Exported
    public void setTotalBetter(Boolean totalBetter) {
        this.totalBetter = totalBetter;
    }

    public Status getStatus() {
        return this.status;
    }

    /**
     * This class is used by Jenkins to define the plugin.
     * 
     * @author jes
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String prqaHome;

        @Override
        public String getDisplayName() {
            return "Programming Research Report";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        String getPrqaHome() {
            return prqaHome;
        }

        @Override
        public PRQANotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            super.configure(req, formData);
            PRQANotifier instance = req.bindJSON(PRQANotifier.class, formData);

            //DEBUG to console
            System.out.println(instance.totalBetter);
            System.out.print(instance.totalMax);
            //DEBUG end

            return instance;
        }

//        @Override
//        public boolean configure(org.kohsuke.stapler.StaplerRequest req, JSONObject json) throws FormException{
//            this.prqaHome = req.getParameter("prqa.prqaHome");
//            save();
//            return true;
//        }
//
//        @Override
//        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
//            req.bindParameters(this);
//            save();
//            return super.configure(req, formData);
//        }
        public DescriptorImpl() {
            super(PRQANotifier.class);
            load();
        }

        public List<String> getReports() {

            return Config.getReports();
        }

        public List<String> getProducts() {
            List<String> s = new ArrayList<String>();
            for (AnalyseTypes a : AnalyseTypes.values()) {
                s.add(a.toString());
            }
            return s;
        }
    }
}
