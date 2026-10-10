package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * WARNING/CRITICAL limits of a metric configured by the quality manager (Environmental Threshold).
 *
 * <p>A value inside the normal range is NORMAL, a value outside the normal range but inside the critical range is
 * WARNING and a value beyond the critical range is CRITICAL. Each side of the range is either complete (normal and
 * critical limit) or not configured, so the air quality can be limited only from above while the temperature is
 * limited on both sides. Thresholds are configured per environment or container; there is no universal range.</p>
 *
 * @param metric      measured quantity, which must accept thresholds
 * @param normalMin   lowest NORMAL value, or null when the metric has no lower limit
 * @param normalMax   highest NORMAL value, or null when the metric has no upper limit
 * @param criticalMin lowest WARNING value; lower values are CRITICAL
 * @param criticalMax highest WARNING value; higher values are CRITICAL
 */
public record EnvironmentalThreshold(MonitoredMetric metric, Double normalMin, Double normalMax,
                                     Double criticalMin, Double criticalMax) {

    public EnvironmentalThreshold {
        if (metric == null) throw new IllegalArgumentException("The metric is required");
        if (!metric.hasThresholds()) {
            throw new IllegalArgumentException("Thresholds cannot be configured for " + metric);
        }
        requireFinite(normalMin, normalMax, criticalMin, criticalMax);
        if ((normalMin == null) != (criticalMin == null) || (normalMax == null) != (criticalMax == null)) {
            throw new IllegalArgumentException(
                    "Each limit of " + metric + " needs both its normal and its critical value");
        }
        if (normalMin == null && normalMax == null) {
            throw new IllegalArgumentException("At least one limit of " + metric + " is required");
        }
        if (normalMin != null && criticalMin >= normalMin) {
            throw new IllegalArgumentException("The critical minimum of " + metric + " must be lower than the normal minimum");
        }
        if (normalMax != null && criticalMax <= normalMax) {
            throw new IllegalArgumentException("The critical maximum of " + metric + " must be higher than the normal maximum");
        }
        if (normalMin != null && normalMax != null && normalMin >= normalMax) {
            throw new IllegalArgumentException("The normal minimum of " + metric + " must be lower than its normal maximum");
        }
    }

    /**
     * Classifies a value of the metric.
     */
    public ThresholdEvaluation evaluate(double value) {
        if (criticalMin != null && value < criticalMin) return new ThresholdEvaluation(EnvironmentalState.CRITICAL, criticalMin);
        if (criticalMax != null && value > criticalMax) return new ThresholdEvaluation(EnvironmentalState.CRITICAL, criticalMax);
        if (normalMin != null && value < normalMin) return new ThresholdEvaluation(EnvironmentalState.WARNING, normalMin);
        if (normalMax != null && value > normalMax) return new ThresholdEvaluation(EnvironmentalState.WARNING, normalMax);
        return new ThresholdEvaluation(EnvironmentalState.NORMAL, null);
    }

    public String unit() {
        return metric.unit();
    }

    private static void requireFinite(Double... values) {
        for (var value : values) {
            if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException("Limits must be finite numbers");
        }
    }
}
