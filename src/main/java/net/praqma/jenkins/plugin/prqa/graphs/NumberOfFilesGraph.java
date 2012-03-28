package net.praqma.jenkins.plugin.prqa.graphs;

import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.status.StatusCategory;

/**
 *
 * @author Praqma
 */
public class NumberOfFilesGraph extends PRQAGraph {
    
    public NumberOfFilesGraph(PRQAContext.QARReportType type) {
        super("Number of Files", type, StatusCategory.TotalNumberOfFiles);
    }
    
    public NumberOfFilesGraph() {
        super("Number of Files", PRQAContext.QARReportType.Suppression, StatusCategory.TotalNumberOfFiles);
    }
    
    @Override
    public void setData(PRQAStatusCollection data) {
        this.data = data;
        this.data.overrideMin(StatusCategory.TotalNumberOfFiles, 0);
    }    
}
