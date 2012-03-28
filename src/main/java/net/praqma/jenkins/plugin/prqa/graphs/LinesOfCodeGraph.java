package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class LinesOfCodeGraph extends PRQAGraph {
    public LinesOfCodeGraph() {
        super("Lines of code", PRQAContext.QARReportType.Suppression, StatusCategory.LinesOfCode);
    }    
}
