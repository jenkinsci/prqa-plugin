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
package net.praqma.jenkins.plugin.prqa;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.prqa.PRQA;
import net.praqma.prqa.products.QAC;

/**
 *
 * @author Praqma
 */
public class PRQARemoteAnalysis implements FilePath.FileCallable<Boolean> {
    
    private BuildListener listener;
    private PRQA prqa;
    private Launcher launcher;
      
    /**
     * Class representing a remote ananlysis job.  
     * 
     * @param prqa Command line application wrapper for Programming Research Applications
     * @param listener Jenkins build listener used for debugging purposes and writing to result log. 
     */
    public PRQARemoteAnalysis(PRQA prqa, BuildListener listener, Launcher launcher) {
        this.listener = listener;
        this.launcher = launcher;
        this.prqa = prqa;
    }
    
    public PRQARemoteAnalysis(String productExecutable, String command, BuildListener listener, Launcher launcher) {
        this.listener = listener;        
        this.prqa = new QAC(productExecutable,command);
        this.launcher = launcher;
    }

    @Override
    public Boolean invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        listener.getLogger().println("Remote file: "+file.getAbsolutePath());
        launcher.launch().cmdAsSingleString(prqa.getProductExecutable() + " " + prqa.getCommand()).start();
        
        
        listener.getLogger().println("Execting command: "+prqa.getProductExecutable() + " " + prqa.getCommand() +"\n"+"In directory: "+file.getPath());
        prqa.execute();
        return true;
    }
    
}
