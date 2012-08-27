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
package net.praqma.jenkins.plugin.prqa.graphs;

import junit.framework.TestCase;
import net.praqma.jenkins.plugin.prqa.graphs.*;
import net.praqma.prqa.PRQAContext;
import net.praqma.prqa.PRQAStatusCollection;
import net.praqma.prqa.status.StatusCategory;
import org.junit.Test;

/**
 *
 * @author Praqma
 */
public class GraphTest extends TestCase {
    @Test
    public void testGraphInitialization() { 
        ComplianceIndexGraphs cGraphs = new ComplianceIndexGraphs();
        System.out.println("Test max values");
        cGraphs.setData(new PRQAStatusCollection());
                
        assertEquals(cGraphs.getData().getOverriddenMax(StatusCategory.FileCompliance), Integer.valueOf(100));
        assertEquals(cGraphs.getData().getOverriddenMax(StatusCategory.ProjectCompliance),Integer.valueOf(100));
        System.out.println("Test min values");
        assertEquals(cGraphs.getData().getOverriddenMin(StatusCategory.FileCompliance),Integer.valueOf(0));
        assertEquals(cGraphs.getData().getOverriddenMin(StatusCategory.ProjectCompliance),Integer.valueOf(0));
        assertNotNull(cGraphs.getTitle());
        assertNotNull(cGraphs.getType());

    }
}
