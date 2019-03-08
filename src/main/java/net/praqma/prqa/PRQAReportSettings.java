package net.praqma.prqa;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;

/**
 * @author Praqma
 */
public class PRQAReportSettings
        implements Serializable, ReportSettings {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public final Collection<String> chosenServer;
    public final EnumSet<PRQAContext.QARReportType> chosenReportTypes;

    public final String projectFile;
    public final String settingsFile;
    public final String fileList;

    public final boolean performCrossModuleAnalysis;
    public final boolean publishToQAV;
    public final boolean enableDependencyMode;
    public final boolean enableDataFlowAnalysis;

    public final String product;

    public PRQAReportSettings(final Collection<String> chosenServer,
                              final String projectFile,
                              final boolean performCrossModuleAnalysis,
                              final boolean publishToQAV,
                              final boolean enableDependencyMode,
                              final boolean enableDataFlowAnalysis,
                              final EnumSet<PRQAContext.QARReportType> chosenReportTypes,
                              final String product,
                              final String settingsFile,
                              final String fileList) {
        this.chosenServer = chosenServer;
        this.projectFile = projectFile;
        this.performCrossModuleAnalysis = performCrossModuleAnalysis;
        this.publishToQAV = publishToQAV;
        this.enableDependencyMode = enableDependencyMode;
        this.enableDataFlowAnalysis = enableDataFlowAnalysis;
        this.chosenReportTypes = chosenReportTypes;
        this.product = product;
        this.settingsFile = settingsFile;
        this.fileList = fileList;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Chosen qaverify server: %s%n", chosenServer));
        builder.append(String.format("Project file: %s%n", projectFile));
        builder.append(String.format("Perform CMA: %s%n", performCrossModuleAnalysis));
        builder.append(String.format("Publish to QAVerify: %s%n", publishToQAV));
        builder.append(String.format("Dependency Analysis: %s%n", enableDependencyMode));
        builder.append(String.format("Data flow analysis: %s%n", enableDataFlowAnalysis));
        return builder.toString();
    }

    @Override
    public String getProduct() {
        return product;
    }

    @Override
    public boolean publishToQAV() {
        return publishToQAV;
    }
}
