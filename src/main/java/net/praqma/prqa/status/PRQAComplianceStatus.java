package net.praqma.prqa.status;

import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaReadingException;
import net.praqma.prqa.parsers.MessageGroup;
import net.praqma.prqa.parsers.Rule;
import net.praqma.prqa.qaframework.QaFrameworkReportSettings;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * This class represent a compliance status readout. 3 values, file compliance,
 * project compliance and number of messages
 *
 * @author jes, man
 */
public class PRQAComplianceStatus
        extends PRQAStatus
        implements Serializable, Comparable<PRQAComplianceStatus> {

    private int messages;
    private int messagesWithinThreshold;
    private Double fileCompliance;
    private Double projectCompliance;
    private Map<Integer, Integer> messagesByLevel = new TreeMap<>();
    private List<MessageGroup> messagesGroups;

    public PRQAComplianceStatus() {
    }

    public PRQAComplianceStatus(final QaFrameworkReportSettings settings) {
        this.settings = settings;
    }

    private String getDisplayName() {
        if(this.settings != null) {
            final String fullPrqaProjectName = settings.getQaProject();
            String pattern = Pattern.quote(File.separator);
            String[] parts = fullPrqaProjectName.split(pattern);
            return parts.length >= 1 ? parts[parts.length - 1] : "";
        }
        return "";
    }

    public PRQAComplianceStatus(int messages,
                                Double fileCompliance,
                                Double projectCompliance) {
        this();
        logger.finest("Constructor called for class PRQAComplianceStatus");
        logger.finest(String.format("Input parameter messages type: %s; value: %s", "int", messages));
        logger.finest(String.format("Input parameter fileCompliance type: %s; value: %s", fileCompliance.getClass(),
                                    fileCompliance));
        logger.finest(
                String.format("Input parameter projectCompliance type: %s; value: %s", projectCompliance.getClass(),
                              projectCompliance));

        this.messages = messages;
        this.fileCompliance = fileCompliance;
        this.projectCompliance = projectCompliance;

        logger.finest("Ending execution of constructor - PRQAComplianceStatus");
    }

    public int getMessages() {
        logger.finest("Starting execution of method - getMessages");
        logger.finest(String.format("Returning value: %s", messages));

        return messages;
    }

    public void setMessages(int messages) {
        logger.finest("Starting execution of method - setMessages");
        logger.finest(String.format("Input parameter messages type: %s; value: %s", "int", messages));

        this.messages = messages;

        logger.finest("Ending execution of method - setMessages");
    }

    public Double getFileCompliance() {
        logger.finest("Starting execution of method - getFileCompliance");
        logger.finest(String.format("Returning value: %s", this.fileCompliance));

        return this.fileCompliance;
    }

    public void setFileCompliance(Double fileCompliance) {
        logger.finest("Starting execution of method - setFileCompliance");
        logger.finest(String.format("Input parameter fileCompliance type: %s; value: %s", fileCompliance.getClass(),
                                    fileCompliance));

        this.fileCompliance = fileCompliance;

        logger.finest("Ending execution of method - setFileCompliance");
    }

    public Double getProjectCompliance() {
        logger.finest("Starting execution of method - getProjectCompliance");
        logger.finest(String.format("Returning value: %s", this.projectCompliance));

        return this.projectCompliance;
    }

    public void setProjectCompliance(Double projCompliance) {
        logger.finest("Starting execution of method - setProjectCompliance");
        logger.finest(String.format("Input parameter projCompliance type: %s; value: %s", projCompliance.getClass(),
                                    projCompliance));

        this.projectCompliance = projCompliance;

        logger.finest("Ending execution of method - setProjectCompliance");
    }

    @Override
    public Number getReadout(StatusCategory cat)
            throws PrqaException {
        logger.finest("Starting execution of method - getReadout");
        logger.finest(String.format("Input parameter cat type: %s; value: %s", cat.getClass(), cat));

        Number output;
        switch (cat) {
            case ProjectCompliance:
                output = this.getProjectCompliance();

                logger.finest(String.format("Returning value: %s", output));

                return output;
            case Messages:
                output = this.getMessages();

                logger.finest(String.format("Returning value: %s", output));

                return output;
            case FileCompliance:
                output = this.getFileCompliance();

                logger.finest(String.format("Returning this.getFileCompliance(): %s", this.getFileCompliance()));

                return output;
            default:
                PrqaReadingException exception = new PrqaReadingException(
                        String.format("Didn't find category %s for class %s", cat, this.getClass()));

                logger.severe(String.format("Exception thrown type: %s; message: %s", exception.getClass(),
                                            exception.getMessage()));

                throw exception;
        }
    }

    @Override
    public void setReadout(StatusCategory category,
                           Number value)
            throws PrqaException {
        logger.finest("Starting execution of method - setReadout");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));
        logger.finest(String.format("Input parameter value type: %s; value: %s", value.getClass(), value));

        switch (category) {
            case ProjectCompliance:
                double prjCompliance = value.doubleValue();

                logger.finest(String.format("Setting projectCompliance to: %s.", prjCompliance));

                setProjectCompliance(prjCompliance);

                logger.finest("Ending execution of method - setReadout");

                break;
            case Messages:
                int msgs = value.intValue();

                logger.finest(String.format("Setting messages to: %s.", msgs));

                setMessages(msgs);

                logger.finest("Ending execution of method - setReadout");

                break;
            case FileCompliance:
                double fileCompl = value.doubleValue();

                logger.finest(String.format("Setting fileCompliance to: %s.", fileCompl));

                setFileCompliance(fileCompl);

                logger.finest("Ending execution of method - setReadout");

                break;
            default:
                PrqaReadingException exception = new PrqaReadingException(
                        String.format("Could not set value of %s for category %s in class %s", value, category,
                                      this.getClass()));

                logger.severe(String.format("Exception thrown type: %s; message: %s", exception.getClass(),
                                            exception.getMessage()));

                throw exception;
        }
    }

    /***
     * Implemented to provide a good reading
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Project Compliance Index : ");
        sb.append((getProjectCompliance() != null ? getProjectCompliance() : "N/A"));
        sb.append("%" + System.getProperty("line.separator"));

        sb.append("File Compliance Index : ");
        sb.append((getFileCompliance() != null ? getFileCompliance() : "N/A"));
        sb.append("%" + System.getProperty("line.separator"));

        sb.append("Diagnostic Count : ");
        sb.append(messages + System.getProperty("line.separator"));
        sb.append("Messages by group:\n");

        if (getMessagesGroups() != null && getMessagesGroups().size() > 0) {

            List<MessageGroup> messagesGroups = getMessagesGroups();
            List<Rule> violatedRules;
            for (MessageGroup messageGroup : messagesGroups) {
                sb.append(messageGroup.getMessageGroupName() + "\n");
                violatedRules = messageGroup.getViolatedRules();
                for (Rule violatedRule : violatedRules) {
                    sb.append(String.format("Rule %s messages (%s)%n", violatedRule.getRuleID(),
                                            violatedRule.getRuleTotalViolations()));
                }
            }
        }

        if (getMessagesByLevel() != null) {
            for (int i : getMessagesByLevel().keySet()) {
                sb.append(String.format("Rule %s messages (%s)%n", i, getMessagesByLevel().get(i)));
            }
        }
        if (notifications != null) {
            for (String note : notifications) {
                sb.append("Notify: " + note + System.getProperty("line.separator"));
            }
        }
        return sb.toString();
    }

    /***
     * Implemented this to decide which one is 'better than last'.
     */
    @Override
    public int compareTo(PRQAComplianceStatus o) {
        if (this == o) {
            return 0;
        }

        if (o == null) {
            return 1;
        }

        if (this.projectCompliance < o.getProjectCompliance() || this.fileCompliance < o.getProjectCompliance() || this.messages > o.getMessages()) {
            return -1;
        } else if (this.projectCompliance > o.getProjectCompliance() || this.fileCompliance > o.getFileCompliance() || this.messages < o.getMessages()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isValid() {
        logger.finest("Starting execution of method - isValid");

        boolean result = this.fileCompliance != null && this.projectCompliance != null;

        logger.finest(String.format("Returning value: %s", result));

        return result;
    }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<h2>Project: %s</h2>", getDisplayName()));
        sb.append(
                "<table cellpadding=\"0\" style=\"border-collapse:separate;border:solid black 1px;border-radius:6px;-moz-border-radius:6px;\">");
        sb.append("<h3>Compliance Summary</h3>");
        sb.append("<thead>");
        sb.append("<tr>");
        sb.append("<th style=\"padding:10px 10px 0px;font-weight:700\">Messages within threshold</th>");
        sb.append("<th style=\"padding:10px 10px 0px;font-weight:700\">All messages</th>");
        sb.append("<th style=\"padding:10px 10px 0px;font-weight:700\">Project Compliance</th>");
        sb.append("<th style=\"padding:10px 10px 0px;font-weight:700\">File Compliance</th>");
        sb.append("</tr>");
        sb.append("</thead>");
        sb.append("<tbody>");
        sb.append("<tr>");
        sb.append("<td style=\"padding:10px;font-weight:400\">")
          .append(getMessagesWithinThreshold())
          .append("</td>");
        sb.append("<td style=\"padding:10px;font-weight:400;\">")
          .append(getMessages())
          .append("</td>");
        sb.append("<td style=\"padding:10px;font-weight:400;\">")
          .append(getProjectCompliance() != null ? getProjectCompliance() : "N/A")
          .append("%</td>");
        sb.append("<td style=\"padding:10px;;font-weight:400\">")
          .append(getFileCompliance() != null ? getFileCompliance() : "N/A")
          .append("%</td>");
        sb.append("</tr>");
        sb.append("</tbody>");
        sb.append("</table>");

        if (getMessagesGroups() != null && getMessagesGroups().size() > 0) {

            sb.append("<table cellpadding=\"0\" style=\"border-collapse:separate;border:solid black 1px;border-radius:6px;-moz-border-radius:6px;\">");
            sb.append("<h3>Messages Summary</h3>");
            sb.append("<thead>");
            sb.append("<tr>");

            List<MessageGroup> messagesGroups = getMessagesGroups();
            List<Rule> violatedRules;
            for (MessageGroup messageGroup : messagesGroups) {

                sb.append("<th style=\"padding-right:5px;\">" + messageGroup.getMessageGroupName()
                                                                            .trim() + ": </th>");
                sb.append("<th style=\"padding-right:5px;\">" + messageGroup.getTotalViolations() + "</th>");
                sb.append("</tr>");
                sb.append("</thead>");
                sb.append("<tbody>");

                violatedRules = messageGroup.getViolatedRules();
                int i = 0;
                for (Rule violatedRule : violatedRules) {
                    if (i % 2 == 0) {
                        sb.append(String.format(
                                "<tr><td style=\"background-color:#CCCCCC\">%s:Rule %s -> </td><td style=\"background-color:#CCCCCC\">%s</td></tr>",
                                i, violatedRule.getRuleID(), violatedRule.getRuleTotalViolations()));
                    } else {
                        sb.append(String.format(
                                "<tr><td style=\"background-color:#FFFFFF\">%s:Rule %s -> </td><td style=\"background-color:#FFFFFF\">%s</td></tr>",
                                i, violatedRule.getRuleID(), violatedRule.getRuleTotalViolations()));
                    }
                    i++;
                }

            }
            sb.append("</tbody>");
            sb.append("</table>");
        } else if (getMessagesByLevel() != null && getMessagesByLevel().size() > 0) {
            sb.append("<table cellpadding=\"0\" style=\"border-collapse:separate;border:solid black 1px;border-radius:6px;-moz-border-radius:6px;\">");
            sb.append("<h3>Messages Summary</h3>");
            sb.append("<thead>");
            sb.append("<tr>");
            sb.append("<th style=\"padding-right:5px;\">Level</th>");
            sb.append("<th style=\"padding-right:5px;\">Number of messages</th>");
            sb.append("</tr>");
            sb.append("</thead>");
            sb.append("<tbody>");
            for (int i : getMessagesByLevel().keySet()) {
                if (i % 2 == 0) {
                    sb.append(String.format(
                            "<tr><td style=\"background-color:#CCCCCC\">%s</td><td style=\"background-color:#CCCCCC\">%s</td></tr>",
                            i, getMessagesByLevel().get(i)));
                } else {
                    sb.append(String.format(
                            "<tr><td style=\"background-color:#FFFFFF\">%s</td><td style=\"background-color:#FFFFFF\">%s</td></tr>",
                            i, getMessagesByLevel().get(i)));
                }

            }
            sb.append("</tbody>");
            sb.append("</table>");
        }
        sb.append("<br/>");
        sb.append("<hr/>");
        return sb.toString();
    }

    public int getMessagesWithinThreshold() {
        return messagesWithinThreshold;
    }

    public List<MessageGroup> getMessagesGroups() {
        return messagesGroups;
    }

    public void setMessagesGroups(List<MessageGroup> messagesGroups) {
        this.messagesGroups = messagesGroups;
    }

    /**
     * @return the messagesByLevel
     */
    public Map<Integer, Integer> getMessagesByLevel() {
        return messagesByLevel;
    }

    public int getMessageCount(int threshold) {
        int cnt = 0;
        Map<Integer, Integer> messagesByLevel = getMessagesByLevel();
        for (int i = threshold; i <= 9; i++) {
            if (messagesByLevel != null && messagesByLevel.containsKey(i)) {
                cnt += messagesByLevel.get(i);
            }
        }
        return cnt;
    }

    public int getMessagesWithinThresholdCount(int thresholdLevel) {
        if (messagesGroups != null && !messagesGroups.isEmpty()) {
            int count = 0;
            for (MessageGroup messageGroup : messagesGroups) {
                count += messageGroup.getMessagesWithinThreshold();
            }
            messagesWithinThreshold = count;
        } else {
            messagesWithinThreshold = getMessageCount(thresholdLevel);
        }
        return messagesWithinThreshold;

    }

    /**
     * @param messagesWithinThreshold the messagesWithinThreshold to set
     */
    public void setMessagesWithinThreshold(int messagesWithinThreshold) {
        this.messagesWithinThreshold = messagesWithinThreshold;
    }

    public void setMessagesWithinThresholdForEachMessageGroup(int thresholdLevel) {
        if (messagesGroups != null && !messagesGroups.isEmpty()) {
            Map<String, Integer> map = new HashMap<>();
            for (MessageGroup messageGroup : messagesGroups) {
                messageGroup.setMessagesWithinThreshold(thresholdLevel, map);
            }
            messagesWithinThreshold = Rule.calc(map);
        }
    }
}
