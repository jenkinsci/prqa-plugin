package net.praqma.jenkins.plugin.prqa.graphs;

import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.status.StatusCategory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Praqma
 */
public abstract class PRQAGraph implements Serializable {
    protected List<StatusCategory> categories;
    protected PRQAStatusCollection data;
    protected String title;
    protected PRQAContext.QARReportType type;
    private static final Logger log = Logger.getLogger(PRQAGraph.class.getName());
   
    public PRQAContext.QARReportType getType() {
        return type;
    }
    
    public void setType(PRQAContext.QARReportType type) {
        this.type = type; 
    }
    
    public List<StatusCategory> getCategories() {
        return categories;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setData(PRQAStatusCollection data) {
        this.data = data;
    }
    
    public PRQAStatusCollection getData() {
        return data;
    }
      
    public boolean containsStatus(StatusCategory cat) {
        return categories.contains(cat);
    }
    
    public void drawGraph(StaplerRequest req, StaplerResponse rsp, DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb, Double thresholdMax) throws IOException {
        try {
        Number max = null;
        Number min = null;
        int width = Integer.parseInt(req.getParameter("width"));
        int height = Integer.parseInt(req.getParameter("height"));
        int tsetting = Integer.parseInt(req.getParameter("tsetting"));
        
        for (StatusCategory category : categories) {
            try {
                max = data.getMax(category);
                min = data.getMin(category);                
            } catch (PrqaException iex) {
                continue;
            }
            
            //Dynamic graph title based on the threshold setting.
            if(category.equals(StatusCategory.Messages)) {
                if(tsetting == 0) {
                    setTitle("Messages per Build");
                } else if(tsetting  == 9) {
                    setTitle("Level 9 Messages per Build");
                } else {
                    setTitle(String.format("Level %s-9 Messages per Build",tsetting));
                }
            }
            log.fine("Iterating using category: "+category);
            if(max != null && min != null) {

                final JFreeChart chart = createChart(dsb.build(), getTitle() == null ? category.toString() : getTitle(), null, thresholdMax != null ? thresholdMax.intValue() : max.intValue(), min.intValue());

                new Graph(-1,width,height) {
                    protected JFreeChart createGraph() {
                        return chart;
                    }
                }.doPng(req,rsp);
            }
        
        }
        
        } catch (RuntimeException ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "drawGraph", "Failed to draw a graph",ex);
            throw ex;
        }
    }
    
    protected JFreeChart createChart( CategoryDataset dataset, String title, String yaxis, int max, int min ) {
        final JFreeChart chart = ChartFactory.createLineChart( title, // chart                                                                                                                                       // title
                        null, // unused
                        yaxis, // range axis label
                        dataset, // data
                        PlotOrientation.VERTICAL, // orientation
                        true, // include legend
                        true, // tooltips
                        false // urls
        );

        final LegendTitle legend = chart.getLegend();

        legend.setPosition( RectangleEdge.BOTTOM );

        chart.setBackgroundPaint( Color.white );

        final CategoryPlot plot = chart.getCategoryPlot();

        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlinePaint( null );
        plot.setRangeGridlinesVisible( true );
        plot.setRangeGridlinePaint( Color.black );

        CategoryAxis domainAxis = new ShiftedCategoryAxis( null );
        plot.setDomainAxis( domainAxis );
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_90 );
        domainAxis.setLowerMargin( 0.0 );
        domainAxis.setUpperMargin( 0.0 );
        domainAxis.setCategoryMargin( 0.0 );

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits( NumberAxis.createIntegerTickUnits() );
        //rangeAxis.setUpperBound( max );
        rangeAxis.setLowerBound( min );
        
        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke( new BasicStroke( 2.0f ) );
        ColorPalette.apply( renderer );
        plot.setInsets( new RectangleInsets( 5.0, 0, 0, 5.0 ) );
        return chart;
    }
   
    public PRQAGraph(String title, PRQAContext.QARReportType type, StatusCategory... category) {
        this.data = new PRQAStatusCollection();
        this.categories = new ArrayList<>();
        this.categories.addAll(Arrays.asList(category));
        this.type = type;
        this.title = title;
    }
}
