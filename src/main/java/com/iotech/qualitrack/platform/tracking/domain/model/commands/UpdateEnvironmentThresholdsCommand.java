package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;

import java.util.List;

/**
 * Replaces the thresholds of an environment, evaluated with the readings of its environmental device (US56, TS42).
 */
public record UpdateEnvironmentThresholdsCommand(Long laboratoryId, Long environmentId,
                                                 List<EnvironmentalThreshold> thresholds) {
    public UpdateEnvironmentThresholdsCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        if (thresholds == null) throw new IllegalArgumentException("The thresholds are required");
        thresholds = List.copyOf(thresholds);
    }
}
