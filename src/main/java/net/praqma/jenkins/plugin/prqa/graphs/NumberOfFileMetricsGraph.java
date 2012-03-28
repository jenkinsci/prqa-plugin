package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class NumberOfFileMetricsGraph extends PRQAGraph {
    public NumberOfFileMetricsGraph() {
        super("Number of File Metrics", PRQAContext.QARReportType.Quality, StatusCategory.NumberOfFileMetrics);
    }
}
