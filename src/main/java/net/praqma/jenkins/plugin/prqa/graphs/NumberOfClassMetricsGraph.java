package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 * @author Praqma
 */

public class NumberOfClassMetricsGraph extends PRQAGraph {
    public NumberOfClassMetricsGraph() {
        super("Number of Class Metrics",PRQAContext.QARReportType.Quality,StatusCategory.NumberOfClassMetrics);
    }
}
