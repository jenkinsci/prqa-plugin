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

import com.google.common.base.Strings;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import net.praqma.prqa.PRQAApplicationSettings;
import net.praqma.prqa.ReportSettings;
import net.praqma.prqa.exceptions.PrqaSetupException;
import net.praqma.prqa.products.Product;
import net.praqma.prqa.products.QACli;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PRQARemoteToolCheck extends MasterToSlaveFileCallable<String> {

	public final BuildListener listener;
	public final boolean isUnix;
	public HashMap<String, String> environment;
	public final PRQAApplicationSettings appSettings;
	public final ReportSettings reportSettings;
	public final Product product;
	public static final String PATH = "Path";

	public PRQARemoteToolCheck(Product product, HashMap<String, String> environment, PRQAApplicationSettings appSettings, ReportSettings reportSettings,
			BuildListener listener, boolean isUnix) {
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
	private static Map<String, String> expandEnvironment(Map<String, String> environment,
														 PRQAApplicationSettings appSettings,
														 ReportSettings reportSetting,
														 boolean isUnix) throws PrqaSetupException {

		if (environment == null) {
			return Collections.emptyMap();
		}
		String delimiter = System.getProperty("file.separator");
		String pathSep = System.getProperty("path.separator");

		String product = reportSetting.getProduct();
		if (Strings.isNullOrEmpty(product)) {
			environment.put(QACli.QAF_BIN_PATH, PRQAApplicationSettings.addSlash(environment.get(QACli.QAF_INSTALL_PATH), delimiter) + "common" + delimiter
					+ "bin");
			return environment;
		}

		String pathVar = "";
		Map<String, String> localEnv = System.getenv();
		for (String s : localEnv.keySet()) {
			if (s.equalsIgnoreCase(PATH)) {
				pathVar = s;
				break;
			}
		}
		String currentPath = localEnv.get(pathVar);

		if (reportSetting.getProduct().equalsIgnoreCase("qac")) {
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
		if (!qarFolder.exists()) {
			throw new PrqaSetupException(String.format("Non existent QAR home directory (%s) - Check your tool settings", qarPath));
		}
		currentPath = qarPath + pathSep + currentPath;

		if (StringUtils.isBlank(appSettings.qavClientHome) && reportSetting.publishToQAV()) {
			throw new PrqaSetupException("You have not configured QA·Verify client home - Check your tool settings");
		}

		if (!StringUtils.isBlank(appSettings.qavClientHome) && reportSetting.publishToQAV()) {
			String qavClientHome;
			if (isUnix) {
				qavClientHome = PRQAApplicationSettings.addSlash(appSettings.qavClientHome, delimiter) + "bin";
			} else {
				qavClientHome = appSettings.qavClientHome;
			}

			File qavClientFolder = new File(qavClientHome);
			if (!qavClientFolder.exists()) {
				throw new PrqaSetupException(String.format("Non existent QA Verify client home directory (%s) - Check your tool settings",
						qavClientHome));
			}

			currentPath = qavClientHome + pathSep + currentPath;
		}

		String qawHome = PRQAApplicationSettings.addSlash(appSettings.qawHome, delimiter) + "bin";
		File qawHomeFolder = new File(qawHome);
		if (!qawHomeFolder.exists()) {
			throw new PrqaSetupException(String.format("Non existent QAW home directory (%s) - Check your tool settings", qawHome));
		}

		currentPath = qawHome + pathSep + currentPath;
		environment.put(pathVar, currentPath);

		return environment;
	}

	@Override
	public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		try {

			Map<String, String> envExpanded = expandEnvironment(environment, appSettings, reportSettings, isUnix);
			return product.getProductVersion(envExpanded, f, isUnix);
		} catch (PrqaSetupException setupException) {
			throw new IOException("Tool misconfiguration detected", setupException);
		}
	}
}
