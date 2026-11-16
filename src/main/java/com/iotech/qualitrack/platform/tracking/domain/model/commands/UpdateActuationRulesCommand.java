package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;

import java.util.List;

/**
 * Replaces the actuation rules of a container monitor (US59, TS45).
 *
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the container monitor is located
 * @param deviceId      container monitor (equipment) to configure
 * @param rules         full set of rules that replaces the current one; an empty list removes them
 */
public record UpdateActuationRulesCommand(Long laboratoryId, Long environmentId, Long deviceId, List<ActuationRule> rules) {
    /**
     * Validates the components.
     *
     * @throws IllegalArgumentException when an identifier is not valid or the rules are missing
     */
    public UpdateActuationRulesCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (rules == null) throw new IllegalArgumentException("The actuation rules are required");
        rules = List.copyOf(rules);
    }
}
