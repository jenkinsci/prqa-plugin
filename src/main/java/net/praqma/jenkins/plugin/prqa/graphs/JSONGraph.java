package net.praqma.jenkins.plugin.prqa.graphs;

import org.json.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A DTO for collating graphable data and transforming to a String-based JSON representation when needed
 */
public class JSONGraph {

    /**
     * Private constants to avoid copy-paste or type issues, plus easy changing if required
     */
    private static final String DATA = "data";
    private static final String LABEL = "label";
    private static final String FILL = "fill";
    private static final String BG_COLOR = "backgroundColor";
    private static final String BORDER_COLOR = "borderColor";
    private static final String COMPLIANCE_LEVELS = "Compliance Levels";
    private static final String MESSAGES_PER_BUILD = "Level %s-9 Messages per Build";
    private static final String THRESHOLD = "Threshold";
    private static final String PROJECT_COMPLIANCE = "Project Compliance";
    private static final String FILE_COMPLIANCE = "File Compliance";
    private static final String MESSAGES = "Messages";
    private static final String THRESHOLD_COLOUR = "#808080";
    private static final String PRIMARY_VALUE_COLOUR = "#ff0000";
    private static final String SECONDARY_VALUE_COLOUR = "#0000cc";

    private String fullPrqaProjectName;
    private boolean isComplianceIndexGraph;
    private List<String> xAxisLabels = new ArrayList<>();
    private List<Double> primaryValues = new ArrayList<>();
    private List<Double> secondaryValues = new ArrayList<>();
    private List<Double> thresholdValues = new ArrayList<>();
    private int lowerMessageSeverityLevel = 0;

    public JSONGraph(final String fullPrqaProjectName, final boolean isComplianceIndexGraph, final int lowerMessageSeverityLevel) {
        this.fullPrqaProjectName = fullPrqaProjectName;
        this.isComplianceIndexGraph = isComplianceIndexGraph;
        this.lowerMessageSeverityLevel = lowerMessageSeverityLevel;
    }

    public String getProjectName() {
        return this.fullPrqaProjectName;
    }

    public String getTitle() {
        return String.format("%s: %s", getDisplayName(), getGraphTypeName());
    }

    public boolean isComplianceIndexGraph() {
        return this.isComplianceIndexGraph;
    }

    public void setxAxisLabels(final List<String> xAxisLabels) {
        this.xAxisLabels = xAxisLabels;
    }

    public void setPrimaryValues(final List<Double> values) {
        this.primaryValues = values;
    }

    public void setSecondaryValues(final List<Double> values) {
        this.secondaryValues = values;
    }

    public void setThresholdValues(final List<Double> values) {
        this.thresholdValues = values;
    }


    @Override
    public String toString() {
        // 'json' is the JSON object the graph/chart lib parses
        JSONObject json = new JSONObject();

        // define the labels along the X-axis for the graph
        json.put("labels", this.xAxisLabels);

        // 'thresholdDataset' contains the data points for the threshold setting of each build
        // NOTE: number of data points must equal the number of x-axis label items in order to be displayed
        JSONObject thresholdDataset = new JSONObject();
        if (this.isComplianceIndexGraph) {
            if (this.thresholdValues.size() == this.xAxisLabels.size()) {
                thresholdDataset.put(LABEL, THRESHOLD);
                thresholdDataset.put(BG_COLOR, THRESHOLD_COLOUR);
                thresholdDataset.put(BORDER_COLOR, THRESHOLD_COLOUR);
                thresholdDataset.put(DATA, this.thresholdValues);
                thresholdDataset.put(FILL, false);
                // add this dataset to 'datasets'
                json.append("datasets",thresholdDataset);
            }
        }

        // 'primaryDataset' contains the data points for the primary values of each build
        // NOTE: number of data points must equal the number of x-axis label items in order to be displayed
        JSONObject primaryDataset = new JSONObject();
        if (this.primaryValues.size() == this.xAxisLabels.size()) {
            if (isComplianceIndexGraph) {
                primaryDataset.put(LABEL, PROJECT_COMPLIANCE);
            } else {
                primaryDataset.put(LABEL, MESSAGES);
            }
        }
        primaryDataset.put(BG_COLOR, PRIMARY_VALUE_COLOUR);
        primaryDataset.put(BORDER_COLOR, PRIMARY_VALUE_COLOUR);
        primaryDataset.put(DATA, this.primaryValues);
        primaryDataset.put(FILL, false);
        json.append("datasets", primaryDataset);

        // 'secondaryDataset' contains the data points for secondary values of each build
        // NOTE 1: ignored for if not a compliance graphs
        // NOTE 2: number of data points must equal the number of x-axis label items in order to be displayed
        if (isComplianceIndexGraph) {
            JSONObject secondaryDataset = new JSONObject();
            if (this.secondaryValues.size() == this.xAxisLabels.size()) {
                secondaryDataset.put(LABEL, FILE_COMPLIANCE);
                secondaryDataset.put(BG_COLOR, SECONDARY_VALUE_COLOUR);
                secondaryDataset.put(BORDER_COLOR, SECONDARY_VALUE_COLOUR);
                secondaryDataset.put(DATA, this.secondaryValues);
                secondaryDataset.put(FILL, false);
            }
            json.append("datasets", secondaryDataset);
        }
        return json.toString();
    }

    private String getDisplayName() {
        String pattern = Pattern.quote(File.separator);
        String[] parts = this.fullPrqaProjectName.split(pattern);
        return parts.length >= 1 ? parts[parts.length - 1] : "";
    }

    private Object getGraphTypeName() {
        if (this.isComplianceIndexGraph) {
            return COMPLIANCE_LEVELS;
        }
        return String.format(MESSAGES_PER_BUILD, this.lowerMessageSeverityLevel);
    }
}

