package net.praqma.prqa.parsers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageGroup
        implements Serializable {

    private String messageGroupName;
    private int totalViolations;
    private List<Rule> violatedRules = new ArrayList<>();
    private int messagesWithinTreshold;

    public MessageGroup(String messageGroupName) {
        this.messageGroupName = messageGroupName;
    }

    public String getMessageGroupName() {
        return messageGroupName;
    }

    public int getTotalViolations() {
        if (totalViolations == 0) {
            Map<String, Integer> map = new HashMap<>();
            for (Rule violatedRule : getViolatedRules()) {
                map.putAll(violatedRule.getMessages());
            }
            totalViolations = Rule.calc(map);
        }
        return totalViolations;
    }

    public List<Rule> getViolatedRules() {
        return violatedRules;
    }

    public void addViolatedRule(Rule rule) {
        this.violatedRules.add(rule);
    }

    public int getMessagesWithinThreshold() {
        return messagesWithinTreshold;
    }

    public void setMessagesWithinThreshold(int thresholdLevel,
                                           Map<String, Integer> total) {
        Map<String, Integer> map = new HashMap<>();
        for (Rule violatedRule : getViolatedRules()) {
            int ruleID = violatedRule.getRuleNumber();
            if (ruleID >= thresholdLevel && ruleID <= 9) {
                map.putAll(violatedRule.getMessages());
                total.putAll(violatedRule.getMessages());
            }
        }
        messagesWithinTreshold = Rule.calc(map);
    }
}
