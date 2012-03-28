package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class NumberOfFunctionGraph extends PRQAGraph {
    public NumberOfFunctionGraph() {
        super("Number of Functions", PRQAContext.QARReportType.Quality, StatusCategory.NumberOfFunctions);
    }
}
