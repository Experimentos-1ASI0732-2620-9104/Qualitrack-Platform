package com.iotech.qualitrack.platform.tracking.domain.model.queries;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;

import java.time.Instant;

/**
 * Gets the readings of a device in a period, oldest first (TS58, TS59).
 *
 * @param deviceId container monitor, or null for the environmental device of the environment
 * @param metric   metric to keep, or null for every metric
 */
public record GetMeasurementsQuery(Long laboratoryId, Long environmentId, Long deviceId, MonitoredMetric metric,
                                   Instant from, Instant to) {
    public GetMeasurementsQuery {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("The period is not valid");
    }
}
