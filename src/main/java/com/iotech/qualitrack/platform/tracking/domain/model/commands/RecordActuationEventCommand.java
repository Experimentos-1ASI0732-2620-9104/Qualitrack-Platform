package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationResult;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;

import java.time.Instant;

/**
 * Records an action executed by a container monitor, synchronized from the Edge (TS56).
 *
 * @param laboratoryId   laboratory that owns the device
 * @param environmentId  environment where the container monitor is located
 * @param deviceId       container monitor (equipment) that executed the action
 * @param action         executed action
 * @param triggerMetric  metric whose condition caused the action, or null when the device reports no cause
 * @param triggerState   condition that caused the action (NORMAL when it recovered), or null without a cause
 * @param result         EXECUTED or FAILED; null is recorded as EXECUTED
 * @param occurredAt     moment the device executed the action
 * @param profileVersion profile version the device applied, when it reports it
 */
public record RecordActuationEventCommand(Long laboratoryId, Long environmentId, Long deviceId, ActuationAction action,
                                          MonitoredMetric triggerMetric, EnvironmentalState triggerState,
                                          ActuationResult result, Instant occurredAt, Long profileVersion) {
    /**
     * Validates the components.
     *
     * @throws IllegalArgumentException when an identifier, the action or the moment is missing or not valid
     */
    public RecordActuationEventCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (action == null) throw new IllegalArgumentException("The action is required");
        if (occurredAt == null) throw new IllegalArgumentException("The time of the action is required");
        if (profileVersion != null && profileVersion < 0) throw new IllegalArgumentException("The profile version cannot be negative");
    }
}
