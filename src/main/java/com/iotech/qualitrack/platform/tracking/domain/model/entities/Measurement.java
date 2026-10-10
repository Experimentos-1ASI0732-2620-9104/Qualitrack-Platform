package com.iotech.qualitrack.platform.tracking.domain.model.entities;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ThresholdEvaluation;
import lombok.Getter;

import java.time.Instant;

/**
 * Reading produced by an IoT device of a laboratory (Measurement).
 *
 * <p>It records the device, the environment where it was taken, the metric with its unit and the moment it was
 * measured. When the profile of the environment or container defines a threshold for the metric, the measurement also
 * keeps the resulting state, the limit it crossed and the profile version used to evaluate it.</p>
 */
@Getter
public class Measurement {

    private Long id;

    private Long laboratoryId;

    private Long environmentId;

    /**
     * IoT device (an equipment of Equipment Management) that produced the reading.
     */
    private Long equipmentId;

    /**
     * Name of the measured metric.
     */
    private String parameterName;

    /**
     * Numeric value; null for an RFID reading.
     */
    private Double value;

    /**
     * Text value of an RFID reading.
     */
    private String textValue;

    private String unit;

    /**
     * Moment of the reading in ISO-8601 format.
     */
    private String timestamp;

    private Instant measuredAt;

    /**
     * NORMAL, WARNING or CRITICAL; null when the metric has no threshold.
     */
    private EnvironmentalState state;

    /**
     * Limit crossed by a WARNING or CRITICAL reading.
     */
    private Double thresholdValue;

    /**
     * Version of the profile used to evaluate the reading, or reported by the device.
     */
    private Long profileVersion;

    /**
     * Moment the platform received the reading.
     */
    private Instant receivedAt;

    public Measurement(Long id, Long laboratoryId, Long environmentId, Long equipmentId, String parameterName,
                       Double value, String textValue, String unit, String timestamp, Instant measuredAt,
                       EnvironmentalState state, Double thresholdValue, Long profileVersion, Instant receivedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.environmentId = environmentId;
        this.equipmentId = equipmentId;
        this.parameterName = parameterName;
        this.value = value;
        this.textValue = textValue;
        this.unit = unit;
        this.measuredAt = measuredAt;
        this.timestamp = timestamp;
        this.state = state;
        this.thresholdValue = thresholdValue;
        this.profileVersion = profileVersion;
        this.receivedAt = receivedAt;
    }

    /**
     * Records a reading received from a device.
     *
     * @param evaluation result of the threshold of the metric, or null when the metric has none
     * @throws IllegalArgumentException when the value does not match the metric
     */
    public static Measurement receive(Long laboratoryId, Long environmentId, Long deviceId, MonitoredMetric metric,
                                      Double value, String textValue, Instant measuredAt,
                                      ThresholdEvaluation evaluation, Long profileVersion) {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("The device is required");
        if (metric == null) throw new IllegalArgumentException("The metric is required");
        if (measuredAt == null) throw new IllegalArgumentException("The measurement time is required");
        if (metric.isText()) {
            if (textValue == null || textValue.isBlank() || textValue.length() > 120) {
                throw new IllegalArgumentException(metric + " needs a text value of up to 120 characters");
            }
            if (value != null) throw new IllegalArgumentException(metric + " has no numeric value");
        } else {
            if (value == null || !Double.isFinite(value)) throw new IllegalArgumentException(metric + " needs a numeric value");
            if (metric.isDetection() && value != 0 && value != 1) {
                throw new IllegalArgumentException(metric + " is 1 when detected and 0 otherwise");
            }
            if (textValue != null) throw new IllegalArgumentException(metric + " has no text value");
        }
        return new Measurement(null, laboratoryId, environmentId, deviceId, metric.name(), value,
                textValue == null ? null : textValue.trim(), metric.unit(), measuredAt.toString(), measuredAt,
                evaluation == null ? null : evaluation.state(), evaluation == null ? null : evaluation.exceededLimit(),
                profileVersion, null);
    }
}
