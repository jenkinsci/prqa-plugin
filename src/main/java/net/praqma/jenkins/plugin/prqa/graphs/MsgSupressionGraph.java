package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class MsgSupressionGraph extends PRQAGraph {
    public MsgSupressionGraph() {
        super("Suppressed messages", PRQAContext.QARReportType.Suppression, StatusCategory.UniqueMessagesSupperessed,StatusCategory.MessagesSuppressed);
    }
}
