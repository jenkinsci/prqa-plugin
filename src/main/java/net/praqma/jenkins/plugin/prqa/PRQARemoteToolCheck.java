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

import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.ReportSettings;
import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.products.Product;
import net.praqma.prqa.products.QACli;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PRQARemoteToolCheck
        extends MasterToSlaveFileCallable<String> {

    public final TaskListener listener;
    public final boolean isUnix;
    public HashMap<String, String> environment;
    public final PRQAApplicationSettings appSettings;
    public final ReportSettings reportSettings;
    public final Product product;
    public static final String PATH = "Path";

    public PRQARemoteToolCheck(Product product,
                               HashMap<String, String> environment,
                               PRQAApplicationSettings appSettings,
                               ReportSettings reportSettings,
                               TaskListener listener,
                               boolean isUnix) {
        this.listener = listener;
        this.isUnix = isUnix;
        this.environment = environment;
        this.appSettings = appSettings;
        this.reportSettings = reportSettings;
        this.product = product;
    }

    /**
     * Expands the environment if the environment field for this object is set.
     * This is only done when the user uses a product configuration.
     */
    private static Map<String, String> expandEnvironment(Map<String, String> environment)
            throws PrqaSetupException {

        if (environment == null) {
            return Collections.emptyMap();
        }
        String delimiter = System.getProperty("file.separator");

        environment.put(QACli.QAF_BIN_PATH, PRQAApplicationSettings.addSlash(environment.get(QACli.QAF_INSTALL_PATH),
                delimiter) + "common" + delimiter + "bin");
        return environment;

    }

    @Override
    public String invoke(File f,
                         VirtualChannel channel)
            throws IOException, InterruptedException {
        try {

            Map<String, String> envExpanded = expandEnvironment(environment);
            return product.getProductVersion(envExpanded, f, isUnix);
        } catch (PrqaSetupException setupException) {
            throw new IOException("Tool misconfiguration detected: " + setupException.getMessage(), setupException);
        }
    }
}
