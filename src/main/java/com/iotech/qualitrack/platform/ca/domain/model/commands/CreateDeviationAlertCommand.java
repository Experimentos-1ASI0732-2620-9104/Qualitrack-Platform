package com.iotech.qualitrack.platform.ca.domain.model.commands;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

import java.time.Instant;

/**
 * Command to register a deviation of an environment or of one of its monitored containers (TS73).
 *
 * <p>The deviation opens a new alert, or is correlated with the open alert of the same device and parameter when the
 * incident continues.</p>
 *
 * @param laboratoryId   the laboratory of the environment
 * @param environmentId  the environment where the deviation was detected
 * @param deviceId       the environmental device or container monitor that detected it; null means the environmental
 *                       device of the environment
 * @param measurementId  the Tracking measurement that showed the deviation, when it comes from a reading
 * @param parameterName  the monitored variable (for example TEMPERATURE or AIR_QUALITY)
 * @param recordedValue  the measured value
 * @param thresholdValue the limit crossed by the value
 * @param unit           the unit of the value and the limit
 * @param severity       the severity of the deviation
 * @param detectedAt     when the deviation was measured
 */
public record CreateDeviationAlertCommand(
        Long laboratoryId,
        Long environmentId,
        Long deviceId,
        Long measurementId,
        String parameterName,
        Double recordedValue,
        Double thresholdValue,
        String unit,
        AlertSeverity severity,
        Instant detectedAt
) {
    /**
     * Compact constructor for CreateDeviationAlertCommand.
     * Enforces Fail-Fast validation.
     */
    public CreateDeviationAlertCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        }
        if (deviceId != null && deviceId <= 0) {
            throw new IllegalArgumentException("deviceId cannot be less than 1");
        }
        if (parameterName == null || parameterName.isBlank()) {
            throw new IllegalArgumentException("parameterName cannot be null or blank");
        }
        if (recordedValue == null || !Double.isFinite(recordedValue)) {
            throw new IllegalArgumentException("recordedValue must be a number");
        }
        if (thresholdValue == null || !Double.isFinite(thresholdValue)) {
            throw new IllegalArgumentException("thresholdValue must be a number");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("unit cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity cannot be null");
        }
        if (detectedAt == null) {
            throw new IllegalArgumentException("detectedAt cannot be null");
        }
        parameterName = parameterName.trim().toUpperCase();
        unit = unit.trim();
    }
}
