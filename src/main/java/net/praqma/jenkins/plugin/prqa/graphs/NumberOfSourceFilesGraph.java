package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class NumberOfSourceFilesGraph extends PRQAGraph {
    public NumberOfSourceFilesGraph() {
        super("Number of source files",PRQAContext.QARReportType.Quality, StatusCategory.NumberOfSourceFiles);
    }

    @Override
    public void setData(PRQAStatusCollection data) {
        this.data = data;
        this.data.overrideMin(StatusCategory.NumberOfSourceFiles, 0);
    }    
}
