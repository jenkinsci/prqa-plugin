package net.praqma.prqa.parsers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule
        implements Serializable {

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private String ruleNumber;
    private int ruleTotalViolations;
    private Map<String, Integer> messages;
    private transient boolean calculated;

    public Rule(String ruleID,
                Map<String, Integer> messages) {
        this.ruleNumber = ruleID;
        this.messages = messages;
    }

    public Rule(String ruleID,
                int ruleTotalViolations) {
        this.ruleNumber = ruleID;
        this.ruleTotalViolations = ruleTotalViolations;
    }

    public static int calc(Map<String, Integer> map) {
        int count = 0;
        for (Integer value : map.values()) {
            count += value;
        }
        return count;
    }

    public String getRuleID() {
        return ruleNumber;
    }

    /**
     * Returns the rule ID of the current Rule to be able to compare against a threshold.
     *
     * @return 0 if there is no number in the rule ID. Otherwise, return the first number in the rule ID.
     */
    public int getRuleNumber() {
        Matcher matcher = DIGITS.matcher(ruleNumber);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    public Map<String, Integer> getMessages() {
        if (messages == null) {
            // compatibility with old serialized data
            messages = new HashMap<>();
            if (ruleTotalViolations > 0) {
                messages.put(ruleNumber, ruleTotalViolations);
            }
        }
        return messages;
    }

    public int getRuleTotalViolations() {
        if (!calculated) {
            ruleTotalViolations = calc(getMessages());
            calculated = true;
        }
        return ruleTotalViolations;
    }

    @Override
    public int hashCode() {
        return ruleNumber.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rule) {
            Rule ruleObj = (Rule) obj;
            return Objects.equals(ruleNumber, ruleObj.ruleNumber);
        }
        return false;
    }
}
