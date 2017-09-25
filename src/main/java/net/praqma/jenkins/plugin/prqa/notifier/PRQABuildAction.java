package net.praqma.jenkins.plugin.prqa.notifier;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.Publisher;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import java.util.HashMap;
import net.praqma.jenkins.plugin.prqa.graphs.PRQAGraph;
import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.status.PRQAComplianceStatus;
import net.praqma.prqa.status.PRQAStatus;
import net.praqma.prqa.status.StatusCategory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Praqma
 */
public class PRQABuildAction implements Action {
    
    private final AbstractBuild<?,?> build;
    private Publisher publisher;
    private PRQAReading result;
    public static final String DISPLAY_NAME = "PRQA";
    public static final String URL_NAME = "PRQA";
       
    public PRQABuildAction() { this.build = null; }
     
    public PRQABuildAction(AbstractBuild<?,?> build) {
        this.build = build;
    }
    
    @Override
    public String getIconFileName() {
        return null;
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
    
    public <T extends Publisher> T getPublisher(Class<T> clazz) {
        return (T)publisher;
    }
    
    public PRQAReading getResult() {
        return this.result;
    }
    
    public Number getThreshold(StatusCategory cat) {
        if (this.result != null && this.result.getThresholds().containsKey(cat)) {
            return result.getThresholds().get(cat);
        }
        return null;
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
    
    /**
     * Fetches the previous PRQA build. Skips builds that were not configured as a PRQA Build. 
     * 
     * Goes to the end of list.
     */ 
    public PRQABuildAction getPreviousAction(AbstractBuild<?,?> base) {
        PRQABuildAction action;
        AbstractBuild<?,?> start = base;
        while(true) {
            start = start.getPreviousNotFailedBuild(); 
            if(start == null) {
                return null;
            }
            action = start.getAction(PRQABuildAction.class);            
            if(action != null) {
                return action;
            }
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
     * Determines weather to draw threhshold graphs. Uses the most recent build as base.
     * @param req
     * @param rsp
     * @return 
     */
    private HashMap<StatusCategory,Boolean>  _doDrawThresholds(StaplerRequest req, StaplerResponse rsp) {
        PRQANotifier notifier = (PRQANotifier)getPublisher();
        HashMap<StatusCategory,Boolean> stats = new HashMap<>();
        if(notifier != null) {             
            String className = req.getParameter("graph");
            PRQAGraph graph =  notifier.getGraph(className);
            for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                if(prqabuild.getResult() != null) {
                    for(StatusCategory cat : graph.getCategories()) {
                        Number threshold = prqabuild.getThreshold(cat);                                                       
                        if(threshold != null) {
                            stats.put(cat, Boolean.TRUE);
                        } else {
                            stats.put(cat, Boolean.FALSE);
                        }
                    }                    
                    return stats;                    
                }
            }
        }
        
        return stats;
    }
       
    /**
     * This function works in the following way:
     * 
     * After choosing your report type, as set of supported graphs are given, which it is up to the user to add. Currently this is done programatically, but given my design, it should be relatively simple to make
     * this possible to edit in the GUI.
     * 
     * If a result is fetched and it does not contain the property to draw the graphs the report demands we simply skip it. This means you can switch report type in a job. You don't need
     * to create a new job if you just want to change reporting mode.
     * 
     * This method catches the PrqaReadingException, when that exception is thrown it means that the we skip the reading and continue. 
     * @param req
     * @param rsp
     * @throws IOException 
     */
    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        PRQANotifier notifier = (PRQANotifier)getPublisher();
        HashMap<StatusCategory,Boolean> drawMatrix = _doDrawThresholds(req, rsp);
               
        if(notifier != null) {
            Integer tSetting = Integer.parseInt(req.getParameter("tsetting"));
            String className = req.getParameter("graph");
            PRQAGraph graph =  notifier.getGraph(className);
            PRQAStatusCollection collection = new PRQAStatusCollection();
            DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<>();
            ChartUtil.NumberOnlyBuildLabel label;
            
            Double tMax = null;
            
            for(PRQABuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
                if(prqabuild.getResult() != null) {
                    label = new ChartUtil.NumberOnlyBuildLabel((Run<?, ?>) prqabuild.build);
                    PRQAReading stat = prqabuild.getResult();
                    for(StatusCategory cat : graph.getCategories()) {
                        Number res;
                        try
                        {
                            PRQAComplianceStatus cs = (PRQAComplianceStatus)stat;
                            if(cat.equals(StatusCategory.Messages)) {
//                                res = cs.getMessageCount(tSetting);
                            	res = cs.getMessagesWithinThresholdCount(tSetting);
                            } else {
                               res = stat.getReadout(cat); 
                            }                            
                        } catch (PrqaException ex) {
                            continue;
                        }
                        
                        if(drawMatrix.containsKey(cat) && drawMatrix.get(cat)) {
                            Number threshold = prqabuild.getThreshold(cat);                        
                            if(threshold != null) {
                                if(tMax == null) {
                                    tMax = threshold.doubleValue();
                                } else if(tMax < threshold.doubleValue()) {
                                    tMax = threshold.doubleValue();
                                }

                                dsb.add(threshold, String.format("%s Threshold", cat.toString()), label);
                            }
                        }
                        dsb.add(res, cat.toString(), label);
                        collection.add(stat);
                    }                   
                }
            }
            
            graph.setData(collection);
            graph.drawGraph(req, rsp, dsb, tMax);
        }
    }
}
