package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class NumberOfFunctionMetricsGraph extends PRQAGraph {
    public NumberOfFunctionMetricsGraph() {
        super("Number of Function Metrics", PRQAContext.QARReportType.Quality, StatusCategory.NumberOfFunctionMetrics);
    }
}
