package com.iotech.qualitrack.platform.tracking.domain.model.commands;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;

import java.util.List;

/**
 * Replaces the temperature, humidity and luminosity thresholds of a container monitor (US57, US58, TS43, TS44).
 */
public record UpdateContainerMonitorThresholdsCommand(Long laboratoryId, Long environmentId, Long deviceId,
                                                      List<EnvironmentalThreshold> thresholds) {
    public UpdateContainerMonitorThresholdsCommand {
        TrackingCommandArguments.requirePositive(laboratoryId, "Laboratory ID");
        TrackingCommandArguments.requirePositive(environmentId, "Environment ID");
        TrackingCommandArguments.requirePositive(deviceId, "Device ID");
        if (thresholds == null) throw new IllegalArgumentException("The thresholds are required");
        thresholds = List.copyOf(thresholds);
    }
}
