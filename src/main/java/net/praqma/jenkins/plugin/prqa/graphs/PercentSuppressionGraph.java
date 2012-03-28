package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.status.StatusCategory;

/**
 * @author Praqma
 */
public class PercentSuppressionGraph extends PRQAGraph {
    public PercentSuppressionGraph() {
        super("Suppressed Messages", PRQAContext.QARReportType.Suppression, StatusCategory.PercentageMessagesSuppressed);
    }
    
    @Override
    public void setData(PRQAStatusCollection data) {
        this.data = data;
        for(StatusCategory s : categories) {
            data.overrideMax(s, 100);
            data.overrideMin(s, 0);
        }
    }
}
