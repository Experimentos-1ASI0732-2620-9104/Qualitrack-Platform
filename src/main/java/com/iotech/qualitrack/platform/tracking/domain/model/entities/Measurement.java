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

    /**
     * Identifier of the stored reading; null until it is saved.
     */
    private Long id;

    /**
     * Laboratory (tenant) that owns the device.
     */
    private Long laboratoryId;

    /**
     * Environment where the reading was taken.
     */
    private Long environmentId;

    /**
     * IoT device (an equipment of Equipment Management) that produced the reading.
     */
    private Long equipmentId;

    /**
     * Name of the measured metric, as in {@link MonitoredMetric#name()}.
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

    /**
     * Fixed unit of the metric, as in {@link MonitoredMetric#unit()}.
     */
    private String unit;

    /**
     * Moment of the reading in ISO-8601 format, kept for the readings stored before {@link #measuredAt} existed.
     */
    private String timestamp;

    /**
     * Moment the device took the reading.
     */
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

    /**
     * Reconstructs a reading from persistence data, without validation.
     */
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
     * <p>A numeric metric needs a finite value (MOTION only 1 or 0) and no text; RFID_TAG needs a text of up to 120
     * characters and no numeric value. The unit is taken from the metric.</p>
     *
     * @param deviceId       device (equipment) that produced the reading
     * @param evaluation     result of the threshold of the metric, or null when the metric has none
     * @param profileVersion profile version used to evaluate the reading or reported by the device, or null
     * @return a new, unsaved measurement
     * @throws IllegalArgumentException when the device, the metric or the moment is missing or the value does not
     *                                  match the metric
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
