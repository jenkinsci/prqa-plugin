package net.praqma.jenkins.plugin.prqa.notifier;

import net.praqma.prqa.parsers.MessageGroup;

public class ValidateTresholdTuple {

    private MessageGroup messageGroup;
    private boolean isValidTreshold;

    public ValidateTresholdTuple(MessageGroup messageGroup,
                                 boolean isValidTreshold) {
        this.messageGroup = messageGroup;
        this.isValidTreshold = isValidTreshold;
    }

    public MessageGroup getMessageGroup() {
        return messageGroup;
    }

    public boolean isValidTreshold() {
        return isValidTreshold;
    }
}
