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

import hudson.Launcher;
import hudson.cli.CreateJobCommand;
import hudson.model.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.maven.wagon.observers.Debug;
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
    
    //TODO: implement this
    //We need to enable "Programming Research Report" element in project configuraion
    @Test public void testCreatePRQAJobConfiguration() {
        assertTrue(true);
    }
    
    //TODO: implement this
    //We need to figure out a way to test 
    @Test public void testExpectedBuildResults() {
        assertTrue(true);
    }
}
