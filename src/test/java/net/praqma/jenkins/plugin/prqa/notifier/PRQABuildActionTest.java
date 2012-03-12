package net.praqma.jenkins.plugin.prqa.notifier;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 *
 * @author Praqma
 */
public class PRQABuildActionTest extends HudsonTestCase {

    @Test public void testGetDisplayName() {
        PRQABuildAction action = new PRQABuildAction();
        assertEquals(action.getDisplayName(), PRQABuildAction.DISPLAY_NAME);       
    } 
    
    @Test public void testGetUrlName() {
        PRQABuildAction action = new PRQABuildAction();
        assertEquals(action.getUrlName(), PRQABuildAction.URL_NAME);
    }
    
    @Test public void testCreatePRQAFreestyleProject() throws IOException, Exception {
        FreeStyleProject fsp = createFreeStyleProject("PRQA Test");
           
        System.out.println(String.format("About to schedule job on port %s",localPort));
        
        FreeStyleBuild fsb = fsp.scheduleBuild2(0).get(60, TimeUnit.SECONDS);
        Result res = fsp.getLastBuild().getResult();
        System.out.println(String.format("Result : %s", res));
        assertBuildStatus(Result.SUCCESS, fsp.getLastBuild());    
        
        assertNotNull(fsp);
        assertNotNull(fsb);
        
        fsb.deleteArtifacts();
        fsb.delete();
        fsp.delete();
        
    }
}
