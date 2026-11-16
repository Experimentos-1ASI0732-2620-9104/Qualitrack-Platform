package com.iotech.qualitrack.platform.tracking.domain.model.queries;

import java.time.Instant;

/**
 * Gets the actions executed by a container monitor in a period, oldest first (TS60).
 *
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the container monitor is located
 * @param deviceId      container monitor (equipment)
 * @param from          start of the period, inclusive
 * @param to            end of the period, inclusive
 */
public record GetActuationEventsQuery(Long laboratoryId, Long environmentId, Long deviceId, Instant from, Instant to) {
    /**
     * Validates the components.
     *
     * @throws IllegalArgumentException when the period is missing or starts after it ends
     */
    public GetActuationEventsQuery {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("The period is not valid");
    }
}
