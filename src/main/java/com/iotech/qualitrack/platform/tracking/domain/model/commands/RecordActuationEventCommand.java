package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationResult;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;

import java.time.Instant;

/**
 * Records an action executed by a container monitor, synchronized from the Edge (TS56).
 */
public record RecordActuationEventCommand(Long laboratoryId, Long environmentId, Long deviceId, ActuationAction action,
                                          MonitoredMetric triggerMetric, EnvironmentalState triggerState,
                                          ActuationResult result, Instant occurredAt, Long profileVersion) {
    public RecordActuationEventCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (action == null) throw new IllegalArgumentException("The action is required");
        if (occurredAt == null) throw new IllegalArgumentException("The time of the action is required");
        if (profileVersion != null && profileVersion < 0) throw new IllegalArgumentException("The profile version cannot be negative");
    }
}
