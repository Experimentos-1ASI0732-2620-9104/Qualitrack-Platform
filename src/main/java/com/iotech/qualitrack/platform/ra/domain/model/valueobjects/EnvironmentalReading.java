package com.iotech.qualitrack.platform.ra.domain.model.valueobjects;

import java.time.Instant;

/**
 * Reading of an IoT device as Reporting &amp; Audit sees it, taken from Tracking &amp; Telemetry through its facade.
 *
 * @param deviceId   the environmental device or container monitor that sent it
 * @param metric     the measured metric
 * @param value      the numeric value, or null for readings without one
 * @param unit       the unit of the value
 * @param state      NORMAL, WARNING or CRITICAL as evaluated by the platform, or null when it was not evaluated
 * @param measuredAt when the device measured it
 */
public record EnvironmentalReading(Long deviceId, String metric, Double value, String unit, String state,
                                   Instant measuredAt) {

    /**
     * @return true when the reading has a finite numeric value that can be aggregated
     */
    public boolean isNumeric() {
        return value != null && Double.isFinite(value) && measuredAt != null;
    }

    /**
     * @return true when the platform evaluated the reading against the thresholds of its profile
     */
    public boolean isEvaluated() {
        return isNumeric() && state != null;
    }

    /**
     * Severity rank of the evaluated condition: NORMAL 0, WARNING 1, CRITICAL 2.
     */
    public int severityRank() {
        return switch (state == null ? "" : state) {
            case "WARNING" -> 1;
            case "CRITICAL" -> 2;
            default -> 0;
        };
    }
}
