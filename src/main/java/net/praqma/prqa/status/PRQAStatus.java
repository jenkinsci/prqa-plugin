/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.status;

import net.praqma.prqa.PRQAReading;
import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.exceptions.PrqaReadingException;
import net.praqma.prqa.qaframework.QaFrameworkReportSettings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Base class for all status objects.
 * <p>
 * Each object holds a list of notifactions associated with that status.
 * <p>
 * And a list of disbaled categories...should be removed in final iteration.
 *
 * @author Praqma
 */
public abstract class PRQAStatus
        implements PRQAReading, Serializable {

    private static final long serialVersionUID = 1L;
    protected List<String> notifications = new ArrayList<>();
    protected HashMap<StatusCategory, Number> thresholds;
    protected static final Logger logger = Logger.getLogger(PRQAStatus.class.getName());
    protected QaFrameworkReportSettings settings;

    @Override
    public void addNotification(String message) {
        logger.finest("Starting execution of method - addNotification");
        logger.finest(String.format("Input parameter message type: %s; value: %s", message.getClass(), message));

        notifications.add(message);

        logger.finest("Ending execution of method - addNotification");
    }

    @Override
    public HashMap<StatusCategory, Number> getThresholds() {
        if (thresholds != null) {
            logger.finest("Starting execution of method - getThresholds");
            logger.finest("Returning HashMap<StatusCategory, Number> thresholds:");
            for (Entry<StatusCategory, Number> entry : thresholds.entrySet()) {
                logger.finest(String.format("    StatusCategory: %s, Number: %s", entry.getKey(), entry.getValue()));
            }

            return thresholds;
        } else {
            return new HashMap<>();
        }
    }

    @Override
    public void setThresholds(HashMap<StatusCategory, Number> thresholds) {
        logger.finest("Starting execution of method - setThresholds");
        logger.finest(String.format("Input parameter thresholds type: %s, values:", thresholds.getClass()));
        for (Entry<StatusCategory, Number> entry : thresholds.entrySet()) {
            logger.finest(String.format("    StatusCategory: %s, Number: %s", entry.getKey(), entry.getValue()));
        }

        this.thresholds = thresholds;

        logger.finest("Ending execution of method - setThresholds");
    }

    /**
     * Gets all associated readouts.
     */
    @Override
    public HashMap<StatusCategory, Number> getReadouts(StatusCategory... categories)
            throws PrqaException {
        logger.finest("Starting execution of method - getReadouts");

        for (StatusCategory category : categories) {
            logger.finest(String.format("    %s", category));
        }
        logger.finest("Attempting to get readouts...");

        HashMap<StatusCategory, Number> map = new HashMap<>();
        for (StatusCategory category : categories) {
            try {
                Number readout = getReadout(category);
                map.put(category, readout);
            } catch (PrqaReadingException ex) {
                logger.severe(String.format("Exception thrown type: %s; message: %s", ex.getClass(), ex.getMessage()));

                throw ex;
            }
        }
        logger.finest("Successfully got all readouts!");
        logger.finest("Returning HashMap<StatusCategory, Number> map:");
        for (Entry<StatusCategory, Number> entry : map.entrySet()) {
            logger.finest(String.format("    StatusCategory: %s, Number: %s", entry.getKey(), entry.getValue()));
        }

        return map;
    }

    /**
     * Tests whether the result contains valid values. in some cases we could
     * end up in situation where an "empty" result would be created.
     *
     * @return true if valid
     */
    public abstract boolean isValid();

    /**
     * Returns a table representation of the the toString method.
     *
     * @return html
     */
    public abstract String toHtml();

    public void setSettings(QaFrameworkReportSettings settings) {
        this.settings = settings;
    }

    public QaFrameworkReportSettings getSettings() {
        return this.settings;
    }

    public Set<StatusCategory> getCategories() {
        if (this.thresholds == null) {
            return new HashSet<StatusCategory>();
        }
        return new HashSet<StatusCategory>(this.thresholds.keySet());
    }
}
