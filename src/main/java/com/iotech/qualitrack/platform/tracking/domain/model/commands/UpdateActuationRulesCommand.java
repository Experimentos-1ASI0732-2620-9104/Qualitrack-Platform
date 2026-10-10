package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;

import java.util.List;

/**
 * Replaces the actuation rules of a container monitor (US59, TS45).
 */
public record UpdateActuationRulesCommand(Long laboratoryId, Long environmentId, Long deviceId, List<ActuationRule> rules) {
    public UpdateActuationRulesCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (rules == null) throw new IllegalArgumentException("The actuation rules are required");
        rules = List.copyOf(rules);
    }
}
