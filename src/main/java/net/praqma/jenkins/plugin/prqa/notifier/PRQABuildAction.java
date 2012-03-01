/*
 * The MIT License
 *
 * Copyright 2012 Praqma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.plugin.prqa.notifier;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.tasks.Publisher;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import net.praqma.prqa.PRQAComplianceStatus;
import net.praqma.prqa.PRQAComplianceStatusCollection;
import net.praqma.prqa.PRQAComplianceStatusCollection.ComplianceCategory;
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

/**
 *
 * @author Praqma
 */
public class PRQABuildAction implements Action {
    
    private final AbstractBuild<?,?> build;
    private Publisher publisher;
    public static final String DISPLAY_NAME = "PRQA";
    public static final String URL_NAME = "PRQA";
    
    public PRQABuildAction() { this.build = null; }
     
    public PRQABuildAction(AbstractBuild<?,?> build) {
        this.build = build;
    }
    
    @Override
    public String getIconFileName() {
        throw new UnsupportedOperationException("No icon file defined yet");
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    /**
     * @return the publisher
     */
    public Publisher getPublisher() {
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
    
    /**
     * Used to cycle through all previous builds.
     * @return 
     */
    public PRQABuildAction getPreviousAction() {      
        return getPreviousAction(build);
    }
    
    public PRQABuildAction getPreviousAction(AbstractBuild<?,?> base) {
        PRQABuildAction action = null;
        AbstractBuild<?,?> start = base;
        while(true) {
            start = start.getPreviousNotFailedBuild();
            if(start == null)
                return null;
            action = start.getAction(PRQABuildAction.class);
            return action;
        }
    }
    
    public PRQAComplianceStatus getBuildActionStatus() {
        return ((PRQANotifier)publisher).getStatus();
    }
    
    
    
    public ComplianceCategory[] getComplianceCategories() {
        return ComplianceCategory.values();
    }
    
    /**
     * Do statistics based on the actions of the build.
     * @param req
     * @param rsp 
     */
    public void doComplianceStatistics(StaplerRequest req, StaplerResponse rsp) throws IOException {
        
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();
        int width = Integer.parseInt(req.getParameter("width"));
        int height = Integer.parseInt(req.getParameter("height"));
        String category = req.getParameter("category");
        String scale = null;
        
        Number max = null;
        Number min = null;
        
        //Gather relevant statistics.
        PRQAComplianceStatusCollection observations = new PRQAComplianceStatusCollection(new ArrayList<PRQAComplianceStatus>());
        observations.overrideMax(PRQAComplianceStatusCollection.ComplianceCategory.FileCompliance, 100);
        observations.overrideMin(PRQAComplianceStatusCollection.ComplianceCategory.FileCompliance, 0);
        
        observations.overrideMax(PRQAComplianceStatusCollection.ComplianceCategory.ProjectCompliance, 100);
        observations.overrideMin(PRQAComplianceStatusCollection.ComplianceCategory.ProjectCompliance, 0);
          
        //Get the minimum and maximum observations from the collected observations.
        /*
        for(PRQAComplianceStatusCollection.ComplianceCategory ccat : PRQAComplianceStatusCollection.ComplianceCategory.values()) {
            if(ccat.equals(PRQAComplianceStatusCollection.ComplianceCategory.valueOf(category))) {
               
                for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                    ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build );
                    PRQAComplianceStatus stat = prqabuild.getBuildActionStatus();
                    dsb.add(stat.getComplianceReadout(ccat), ccat.toString(), label);
                    observations.add(stat);         
                }
                
                max = observations.getMax(ccat);
                min = observations.getMin(ccat);
            }
        }
        */
        
        if(ComplianceCategory.valueOf(category).equals(ComplianceCategory.Messages)) {
            for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                    ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build );
                    PRQAComplianceStatus stat = prqabuild.getBuildActionStatus();
                    dsb.add(stat.getComplianceReadout(ComplianceCategory.valueOf(category)), ComplianceCategory.valueOf(category).toString(), label);
                    observations.add(stat);         
            }
            max = observations.getMax(ComplianceCategory.valueOf(category));
            min = observations.getMin(ComplianceCategory.valueOf(category));
            ChartUtil.generateGraph( req, rsp, createChart( dsb.build(), category, scale, max.intValue(), min.intValue()), width, height );
        }
        
        if(ComplianceCategory.valueOf(category).equals(ComplianceCategory.ProjectCompliance)) {
            
            for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                    ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build );
                    PRQAComplianceStatus stat = prqabuild.getBuildActionStatus();
                    dsb.add(stat.getComplianceReadout(ComplianceCategory.valueOf(category)), ComplianceCategory.valueOf(category).toString(), label);
                    dsb.add(stat.getComplianceReadout(ComplianceCategory.FileCompliance), ComplianceCategory.FileCompliance.toString(), label);
                    observations.add(stat);         
            }
            
            max = observations.getMax(ComplianceCategory.valueOf(category));
            min = observations.getMin(ComplianceCategory.valueOf(category)); 
            ChartUtil.generateGraph( req, rsp, createChart( dsb.build(), "Project Compliance Levels", scale, max.intValue(), min.intValue()), width, height );
        }             
    }
      
    private JFreeChart createChart( CategoryDataset dataset, String title, String yaxis, int max, int min ) {

            final JFreeChart chart = ChartFactory.createLineChart( title, // chart
                                                                                                                                            // title
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
            rangeAxis.setUpperBound( max );
            rangeAxis.setLowerBound( min );

            final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setBaseStroke( new BasicStroke( 2.0f ) );
            ColorPalette.apply( renderer );

            plot.setInsets( new RectangleInsets( 5.0, 0, 0, 5.0 ) );

            return chart;
    }

}
