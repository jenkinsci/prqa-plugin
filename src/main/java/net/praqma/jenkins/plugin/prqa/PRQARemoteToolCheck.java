/*
 * The MIT License
 *
 * Copyright 2013 Praqma.
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

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.PRQAReportSettings;
import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.products.Product;

/**
 *
 * @author Praqma
 */
public class PRQARemoteToolCheck implements FileCallable<String> {
    
    public final BuildListener listener;
    public final boolean isUnix;
    public HashMap<String,String> environment;
    public final PRQAApplicationSettings appSettings;
    public final PRQAReportSettings reportSettings;
    public final Product product;
    
    public PRQARemoteToolCheck() {         
        this.listener = null;
        this.isUnix = false;
        this.environment = null;
        this.appSettings = null;
        this.reportSettings = null;        
        this.product = null;
    }
    
    public PRQARemoteToolCheck(Product product, HashMap<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSettings, BuildListener listener, boolean isUnix) {
        this.listener = listener;
        this.isUnix = isUnix;
        this.environment = environment;
        this.appSettings = appSettings;
        this.reportSettings = reportSettings;
        this.product = product;
    }
    
    public HashMap<String,String> expandEnvironment(HashMap<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSetting, boolean isUnix) {
        String pathVar = "path";
        Map<String,String> localEnv = System.getenv();
        
        for(String s : localEnv.keySet()) {
            if(s.equalsIgnoreCase(pathVar)) {
                pathVar = s;
                break;
            }
        }
        
        String currentPath = localEnv.get(pathVar);

        String delimiter = System.getProperty("file.separator");
        String pathSep = System.getProperty("path.separator");
        
        if(environment != null) {
            if(reportSetting.product.equalsIgnoreCase("qac")) {
                String slashPath = PRQAApplicationSettings.addSlash(environment.get("QACPATH"), delimiter);
                environment.put("QACPATH", slashPath);       

                String qacBin = PRQAApplicationSettings.addSlash(environment.get("QACPATH"), delimiter) + "bin";
                environment.put("QACBIN", qacBin);
                environment.put("QACHELPFILES", environment.get("QACPATH") + "help");

                currentPath = environment.get("QACBIN") + pathSep + currentPath;
                environment.put("QACTEMP", System.getProperty("java.io.tmpdir"));                
            } else {
                String slashPath = PRQAApplicationSettings.addSlash(environment.get("QACPPPATH"), delimiter);
                environment.put("QACPPPATH", slashPath);

                String qacppBin = PRQAApplicationSettings.addSlash(environment.get("QACPPPATH"), delimiter) + "bin";
                environment.put("QACPPBIN", qacppBin);
                environment.put("QACPPHELPFILES", environment.get("QACPPPATH") + "help");

                currentPath = environment.get("QACPPBIN") + pathSep + currentPath;
                environment.put("QACPPTEMP", System.getProperty("java.io.tmpdir"));

            }
            
            currentPath = PRQAApplicationSettings.addSlash(appSettings.qarHome, delimiter) + "bin" + pathSep + currentPath;
            if(isUnix) {
                currentPath = PRQAApplicationSettings.addSlash(appSettings.qavClientHome, delimiter) + "bin" + pathSep + currentPath;
            } else {
                currentPath = appSettings.qavClientHome + pathSep + currentPath;
            }
            currentPath = PRQAApplicationSettings.addSlash(appSettings.qawHome, delimiter) + "bin" + pathSep + currentPath;
            environment.put(pathVar, currentPath);
            
        }
        return environment;      
    }
    
    @Override
    public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            listener.getLogger().println("Getting product version for QAW!");
            return product.getProductVersion(expandEnvironment(environment, appSettings, reportSettings, isUnix), f);
        } catch (PrqaSetupException setupException) {
            listener.getLogger().println("Throwing exception");
            throw new IOException("Tool misconfiguration detected", setupException);
        }
    }
  
}
