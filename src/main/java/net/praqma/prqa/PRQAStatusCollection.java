package net.praqma.prqa;

import net.praqma.prqa.exceptions.PrqaException;
import net.praqma.prqa.status.StatusCategory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Praqma
 */
public class PRQAStatusCollection
        extends ArrayList<PRQAReading>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<StatusCategory, Number> overrideMinimum = new EnumMap<>(StatusCategory.class);
    private Map<StatusCategory, Number> overrideMaximum = new EnumMap<>(StatusCategory.class);
    private static final Logger logger;

    static {
        logger = Logger.getLogger(PRQAStatusCollection.class.getName());
    }

    public PRQAStatusCollection() {
        this(new ArrayList<PRQAReading>());
    }

    public PRQAStatusCollection(ArrayList<PRQAReading> collection) {
        logger.finest("Constructor called for class PRQAStatusCollection(ArrayList<PRQAReading> collection)");
        for (PRQAReading e : collection) {
            logger.finest(String.format("    %s", e));
        }

        this.addAll(collection);
    }

    public PRQAStatusCollection(PRQAStatusCollection collection) {
        logger.finest("Constructor called for class PRQAStatusCollection(PRQAStatusCollection collection)");
        for (PRQAReading e : collection) {
            logger.finest(String.format("    %s", e));
        }

        this.addAll(collection);
    }

    /**
     * Implemented a collection method to gather extremities from a given set
     * of collected compliance statistics.
     *
     * @param category for extremities
     * @return extremity
     * @throws PrqaException when exception
     */
    public final Number getMax(StatusCategory category)
            throws PrqaException {
        logger.finest("Starting execution of method - getMax");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));

        if (getOverriddenMax(category) != null) {
            Number output = getOverriddenMax(category);

            logger.finest(String.format("Returning overridden max for StatusCategory %s, value: %s", category, output));

            return output;
        }

        int max = Integer.MIN_VALUE;
        int tmp = 0;

        logger.finest(String.format("Searching for maximum value for StatusCategory %s...", category));

        for (PRQAReading s : this) {
            try {
                tmp = s.getReadout(category) == null ? 0 : s.getReadout(category)
                                                            .intValue();
            } catch (PrqaException iex) {
                logger.severe(
                        String.format("Exception thrown type: %s; message: %s", iex.getClass(), iex.getMessage()));

                throw iex;
            }

            if (tmp >= max) {
                max = tmp;
            }
        }

        logger.finest(String.format("Returning max from StatusCategory %s, value: %s", category, max));

        return max;
    }

    /**
     * Implemented a collection method to gather extremities from a given set
     * of collected compliance statistics.
     *
     * @param category for extremities
     * @return a number indicating the smallest given observation for the
     * specified category.
     * @throws PrqaException when exception
     */
    public final Number getMin(StatusCategory category)
            throws PrqaException {
        logger.finest("Starting execution of method - getMin");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));

        if (getOverriddenMin(category) != null) {
            Number output = getOverriddenMin(category);

            logger.finest(String.format("Returning overridden min for StatusCategory %s, value: %s", category, output));

            return output;
        }

        int min = Integer.MAX_VALUE;
        int tmp = 0;

        logger.finest(String.format("Searching for minimum value for StatusCategory %s...", category));

        for (PRQAReading s : this) {
            try {
                tmp = s.getReadout(category) == null ? 0 : s.getReadout(category)
                                                            .intValue();
            } catch (PrqaException iex) {
                logger.severe(
                        String.format("Exception thrown type: %s; message: %s", iex.getClass(), iex.getMessage()));

                throw iex;
            }
            if (tmp <= min) {
                min = tmp;
            }
        }

        logger.finest(String.format("Returning min from StatusCategory %s, value: %s", category, min));

        return min;
    }

    /**
     * Implemented methods to override the min and max values, these are used in
     * the graphing part of the project. You override specific performance
     * metrics.
     *
     * @param category category
     * @param value value
     */
    public void overrideMin(StatusCategory category,
                            Number value) {
        logger.finest("Starting execution of method - overrideMin");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));
        logger.finest(String.format("Input parameter value type: %s; value: %s", value.getClass(), value));

        overrideMinimum.put(category, value);
    }

    public void overrideMax(StatusCategory category,
                            Number value) {
        logger.finest("Starting execution of method - overrideMax");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));
        logger.finest(String.format("Input parameter value type: %s; value: %s", value.getClass(), value));

        overrideMaximum.put(category, value);
    }

    public Number getOverriddenMax(StatusCategory category) {
        logger.finest("Starting execution of method - getOverriddenMax");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));

        if (overrideMaximum.containsKey(category)) {
            Number output = overrideMaximum.get(category);

            logger.finest(String.format("Returning overridden max for StatusCategory %s, value: %s", category, output));

            return output;
        }

        logger.finest(String.format("Maximum for StatusCategory %s isn't set, returning null", category));

        return null;
    }

    public Number getOverriddenMin(StatusCategory category) {
        logger.finest("Starting execution of method - getOverriddenMin");
        logger.finest(String.format("Input parameter category type: %s; value: %s", category.getClass(), category));
        if (overrideMinimum.containsKey(category)) {
            Number output = overrideMinimum.get(category);
            logger.finest(String.format("Returning overridden min for StatusCategory %s, value: %s", category, output));
            return output;
        }
        logger.finest(String.format("Maximum for StatusCategory %s isn't set, returning null", category));
        return null;
    }

    public final void clearOverrides() {
        logger.finest("Starting execution of method - clearOverrides");

        overrideMaximum.clear();
        overrideMinimum.clear();

        logger.finest("Overrides cleared.");
    }
}
