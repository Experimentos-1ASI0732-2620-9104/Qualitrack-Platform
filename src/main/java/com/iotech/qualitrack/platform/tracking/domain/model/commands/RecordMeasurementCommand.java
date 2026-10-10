package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;

import java.time.Instant;

/**
 * Records a reading synchronized from the Edge (TS54, TS55).
 *
 * @param deviceId       container monitor that measured, or null for the environmental device of the environment
 * @param value          numeric value; null for an RFID reading
 * @param textValue      RFID tag read; null for numeric metrics
 * @param profileVersion profile version the device used, when it reports it
 */
public record RecordMeasurementCommand(Long laboratoryId, Long environmentId, Long deviceId, MonitoredMetric metric,
                                       Double value, String textValue, Instant measuredAt, Long profileVersion) {
    public RecordMeasurementCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        if (deviceId != null) TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (metric == null) throw new IllegalArgumentException("The metric is required");
        if (measuredAt == null) throw new IllegalArgumentException("The measurement time is required");
        if (profileVersion != null && profileVersion < 0) throw new IllegalArgumentException("The profile version cannot be negative");
    }
}
