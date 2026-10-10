package com.iotech.qualitrack.platform.ra.domain.model.entities;

import lombok.Getter;

/**
 * Domain entity representing a single measured data point within a deviation trend.
 *
 * <p>Each data point stores a recorded value, the condition the platform evaluated for it and, when known, its
 * acceptable lower and upper thresholds at a specific timestamp.</p>
 */
@Getter
public class TrendDataPoint {

    /**
     * Timestamp when the measurement was recorded.
     */
    private String timestamp;

    /**
     * Actual recorded value.
     */
    private Double recordedValue;

    /**
     * Maximum accepted value, when known.
     */
    private Double upperThreshold;

    /**
     * Minimum accepted value, when known.
     */
    private Double lowerThreshold;

    /**
     * NORMAL, WARNING or CRITICAL as evaluated by Tracking &amp; Telemetry, or null when it was not evaluated.
     */
    private String state;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public TrendDataPoint() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a data point with its thresholds.
     *
     * @param timestamp the recorded timestamp
     * @param recordedValue the measured value
     * @param upperThreshold the maximum accepted value
     * @param lowerThreshold the minimum accepted value
     */
    public TrendDataPoint(
            String timestamp,
            Double recordedValue,
            Double upperThreshold,
            Double lowerThreshold
    ) {
        this(timestamp, recordedValue, upperThreshold, lowerThreshold, null);
    }

    /**
     * Creates a data point with the condition evaluated for the reading.
     *
     * @param timestamp the recorded timestamp
     * @param recordedValue the measured value
     * @param upperThreshold the maximum accepted value, or null
     * @param lowerThreshold the minimum accepted value, or null
     * @param state the evaluated condition, or null
     */
    public TrendDataPoint(
            String timestamp,
            Double recordedValue,
            Double upperThreshold,
            Double lowerThreshold,
            String state
    ) {
        this.timestamp = timestamp;
        this.recordedValue = recordedValue;
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.state = state;
    }

    /**
     * Indicates whether the data point is a deviation: its evaluated condition is WARNING or CRITICAL, or its value
     * is outside the known thresholds.
     *
     * @return true if the point is a deviation
     */
    public boolean isDeviation() {
        if (state != null) {
            return "WARNING".equals(state) || "CRITICAL".equals(state);
        }
        if (recordedValue == null || upperThreshold == null || lowerThreshold == null) {
            return false;
        }
        return recordedValue > upperThreshold || recordedValue < lowerThreshold;
    }
}
