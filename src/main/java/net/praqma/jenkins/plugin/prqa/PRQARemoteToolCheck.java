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
import net.praqma.prqa.products.QAC;
import net.praqma.prqa.products.QACpp;
import org.apache.commons.lang.StringUtils;

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
    
    
    /**
     * Class that performs the remote tool check
     * @param product
     * @param environment
     * @param appSettings
     * @param reportSettings
     * @param listener
     * @param isUnix 
     */
    public PRQARemoteToolCheck(Product product, HashMap<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSettings, BuildListener listener, boolean isUnix) {
        this.listener = listener;
        this.isUnix = isUnix;
        this.environment = environment;
        this.appSettings = appSettings;
        this.reportSettings = reportSettings;
        this.product = product;
    }
    
    /**
     * Expands the environment if the environment field for this object is set. This is only done when the user uses a product configuration. 
     * @param environment
     * @param appSettings
     * @param reportSetting
     * @param isUnix
     * @return
     * @throws PrqaSetupException 
     */
    public HashMap<String,String> expandEnvironment(HashMap<String,String> environment, PRQAApplicationSettings appSettings, PRQAReportSettings reportSetting, boolean isUnix) throws PrqaSetupException {
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
            
            
            String qarPath = PRQAApplicationSettings.addSlash(appSettings.qarHome, delimiter) + "bin";
            File qarFolder = new File(qarPath);
            if(!qarFolder.exists()) {
                throw new PrqaSetupException( String.format( "Non existant QAR home directory (%s) - Check your tool settings", qarPath) );
            }
            
            currentPath = qarPath + pathSep + currentPath;
            
            if(StringUtils.isBlank(appSettings.qavClientHome) && reportSetting.publishToQAV) {
                throw new PrqaSetupException("You have not configured QA·Verify client home - Check your tool settings");
            }
            
            if(!StringUtils.isBlank(appSettings.qavClientHome) && reportSetting.publishToQAV) {
                String qavClientHome = null;
                if(isUnix) {
                    qavClientHome = PRQAApplicationSettings.addSlash(appSettings.qavClientHome, delimiter) + "bin";
                } else {
                    qavClientHome = appSettings.qavClientHome;
                }
                
                File qavClientFolder = new File(qavClientHome);
                if(!qavClientFolder.exists()) {
                    throw new PrqaSetupException( String.format( "Non existant QA Verify client home directory (%s) does not exist - Check your tool settings", qavClientHome) );
                }

                currentPath = qavClientHome + pathSep + currentPath;
            }
            
            String qawHome = PRQAApplicationSettings.addSlash(appSettings.qawHome, delimiter) + "bin";
            File qawHomeFolder = new File(qawHome);
            if(!qawHomeFolder.exists()) {
                throw new PrqaSetupException( String.format( "Non existant QAW home directory (%s) - Check your tool settings", qawHome) );
            }
            
            currentPath = qawHome + pathSep + currentPath;
            environment.put(pathVar, currentPath);
            
        }
        return environment;      
    }        
    
    private void _checkImportantEnvVars(HashMap<String,String> env, Product product) throws PrqaSetupException {
        if(env != null) {
            if(product instanceof QAC) {            
                for(String s : QAC.envVarsForTool) {
                    String value = env.get(s);
                    if(value == null) {
                        throw new PrqaSetupException(String.format("The enviroment variable %s is not defined"));
                    }                
                    File f = new File(value);
                    if(!f.exists()) {
                        throw new PrqaSetupException(String.format("Configuration error - Check your QA·C Product installation path%nThe enviroment created points to a non-existing location%nCheck your tool settings%nThe tool location missing was: %s",f.getAbsolutePath()));
                    }
                }            
            } else if(product instanceof QACpp) {            
                for(String s : QACpp.envVarsForTool) {
                    String value = env.get(s);
                    if(value == null) {
                        throw new PrqaSetupException(String.format("The enviroment variable %s is not defined"));
                    }
                    File f = new File(value);
                    if(!f.exists()) {
                        throw new PrqaSetupException(String.format("Configuration error - Check your QA·C++ Product installation path%nThe enviroment created points to a non-existing location%n Check your tool settings%nThe tool location missing was: %s",f.getAbsolutePath()));
                    }
                }
            }
        }
    }
    
    private void _checkBinaryMatch(HashMap<String,String> env, Product product) throws PrqaSetupException {
        String pathSep = System.getProperty("file.separator");
        if(env != null) {
            if(isUnix) {
                if(product instanceof QAC) {
                    File f = new File(env.get("QACBIN")+pathSep+"qac");
                    String path = f.getPath();
                    if(!f.exists()) {
                        throw new PrqaSetupException( String.format( "QA·C was selected as product, but no qac binary found in location: %s", path) );
                    }
                } else {
                    File f = new File(env.get("QACPPBIN")+pathSep+"qacpp");
                    String path = f.getPath();
                    if(!f.exists()) {
                        throw new PrqaSetupException( String.format( "QA·C++ was selected as product, but no qacpp binary found in location: %s", path));
                    } 
                } 
            } else {
                if(product instanceof QAC) {
                    File f = new File(env.get("QACBIN")+pathSep+"qac.exe");
                    String path = f.getPath();
                    if(!f.exists()) {
                        throw new PrqaSetupException( String.format( "QA·C was selected as product, but no qac binary found in location: %s", path) );
                    }
                } else {
                    File f = new File(env.get("QACPPBIN")+pathSep+"qacpp.exe");
                    String path = f.getPath();
                    if(!f.exists()) {
                        throw new PrqaSetupException( String.format( "QA·C++ was selected as product, but no qacpp binary found in location: %s", path));
                    } 
                }
            }
        }
    }
    
    @Override
    public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            
            HashMap<String,String> envExpanded = expandEnvironment(environment, appSettings, reportSettings, isUnix);
            _checkImportantEnvVars(envExpanded, product);
            if(product instanceof QAC || product instanceof QACpp) {
                _checkBinaryMatch(envExpanded, product);
            }
            return product.getProductVersion(envExpanded, f, isUnix);
        } catch (PrqaSetupException setupException) {            
            throw new IOException("Tool misconfiguration detected", setupException);
        }
    }
  
}
