package net.praqma.prqa;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * @author jes
 */
public class PRQAContext
        implements Serializable {

    public enum ComparisonSettings {
        None,
        Threshold,
        Improvement;

        @Override
        public String toString() {
            switch (this) {
                default:
                    return this.name();
            }
        }
    }

    public enum QARReportType {
        Compliance,
        CodeReview,
        Suppression;

        public static final EnumSet<QARReportType> OPTIONAL_TYPES = EnumSet.of(CodeReview, Suppression);
        public static final EnumSet<QARReportType> REQUIRED_TYPES = EnumSet.of(Compliance);
    }
}
