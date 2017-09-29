package net.praqma.jenkins.plugin.prqa.graphs;

import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.status.StatusCategory;
import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * @author Praqma
 */
public class ComplianceIndexGraphs
        extends PRQAGraph {
    public ComplianceIndexGraphs() {
        super("Compliance Levels",
              PRQAContext.QARReportType.Compliance,
              StatusCategory.FileCompliance,
              StatusCategory.ProjectCompliance);
    }

    @Override
    public void setData(PRQAStatusCollection data) {
        this.data = data;
        this.data.overrideMax(StatusCategory.FileCompliance,
                              100);
        this.data.overrideMin(StatusCategory.FileCompliance,
                              0);
        this.data.overrideMax(StatusCategory.ProjectCompliance,
                              100);
        this.data.overrideMin(StatusCategory.ProjectCompliance,
                              0);
    }

    @Override
    public void drawGraph(StaplerRequest req,
                          StaplerResponse rsp,
                          DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb,
                          Double tMax)
            throws
            IOException {
        Number max = null;
        Number min = null;
        int width = Integer.parseInt(req.getParameter("width"));
        int height = Integer.parseInt(req.getParameter("height"));

        for (StatusCategory category : categories) {
            try {
                max = data.getMax(category);
                min = data.getMin(category);
            } catch (PrqaException iex) {
                // noop
            }
        }
        if (max != null && min != null) {
            final JFreeChart chart = createChart(dsb.build(),
                                                 getTitle(),
                                                 null,
                                                 max.intValue(),
                                                 min.intValue());

            new Graph(-1,
                      width,
                      height) {
                protected JFreeChart createGraph() {
                    return chart;
                }
            }.doPng(req,
                    rsp);
        }
    }
}
