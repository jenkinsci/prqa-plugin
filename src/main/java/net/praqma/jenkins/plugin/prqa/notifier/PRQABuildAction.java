package net.praqma.jenkins.plugin.prqa.notifier;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.tasks.Publisher;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import net.praqma.jenkins.plugin.prqa.PrqaException;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.status.StatusCategory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Praqma
 */
public class PRQABuildAction implements Action {
    
    private final AbstractBuild<?,?> build;
    private Publisher publisher;
    private PRQAReading result;
    public static final String DISPLAY_NAME = "PRQA";
    public static final String URL_NAME = "PRQA";
    public static final String ICON_NAME="/plugin/prqa-plugin/images/32x32/prqa.ico";
    
    public PRQABuildAction() { this.build = null; }
     
    public PRQABuildAction(AbstractBuild<?,?> build) {
        this.build = build;
    }
    
    @Override
    public String getIconFileName() {
        return ICON_NAME;
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
    
    public PRQAReading getResult() {
        return this.result;
    }
    
    /**
     * Converts the result of the interface to a concrete implementation. Returns null in cases where it is not possible.
     * 
     * This check is needed if you for some reason decide to switch report type on the same job, since each report has it's own implementation
     * of a status. We need to do a check on all the collecte results to get those that fits the current job profile. 
     * @param <T>
     * @param clazz
     * @return 
     */
    public <T extends PRQAStatus> T getResult(Class<T> clazz) {
        try {
            if(this.result.getClass().isAssignableFrom(clazz)) {
                return (T)this.result;
            } else {
                return null;
            }
        } catch (NullPointerException nex) {
            return null;
        }      
    }
        
    public void setResult(PRQAReading result) {
        this.result = result;
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
    
    
    
    public PRQAReading getBuildActionStatus() {
        return this.result;
    }
    
    public <T extends PRQAStatus> T getBuildActionStatus(Class<T> clazz) {
        return (T)this.result;
    }
      
    public StatusCategory[] getComplianceCategories() {
        return StatusCategory.values();
    }
       
    /**
     * This function works in the following way:
     * 
     * Given is a grapclass, which it is up to the user to add. Currently this is done programatically, but given my design, it should be relatively simple to make
     * thispossible to edit in the GUI.
     * 
     * This method catches the PrqaReadingException, when that exception is thrown it means that the we skip the reading and continue. 
     * @param req
     * @param rsp
     * @throws IOException 
     */
    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        PRQANotifier notifier = (PRQANotifier)getPublisher();
        if(notifier != null) {
            String className = req.getParameter("graph");
            PRQAGraph graph =  notifier.getGraph(className);
            PRQAStatusCollection collection = new PRQAStatusCollection();
            DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();
            ChartUtil.NumberOnlyBuildLabel label = null;
                        
            for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                if(prqabuild.getResult() != null) {
                    label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build);
                    PRQAReading stat = prqabuild.getResult();
                    for(StatusCategory cat : graph.getCategories()) {
                        Number res = null;
                        try
                        {
                            res = stat.getReadout(cat);
                        } catch (PrqaException.PrqaReadingException ex) {
                            continue;
                        }                        
                        
                        dsb.add(res, cat.toString(), label);
                        collection.add(stat);
                    }                   
                }
            }
            
            graph.setData(collection);
            graph.drawGraph(req, rsp, dsb);
        }
    }
}
