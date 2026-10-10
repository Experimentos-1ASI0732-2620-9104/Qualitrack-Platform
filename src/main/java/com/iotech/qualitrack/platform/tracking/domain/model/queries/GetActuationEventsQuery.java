package com.iotech.qualitrack.platform.tracking.domain.model.queries;

import java.time.Instant;

/**
 * Gets the actions executed by a container monitor in a period, oldest first (TS60).
 */
public record GetActuationEventsQuery(Long laboratoryId, Long environmentId, Long deviceId, Instant from, Instant to) {
    public GetActuationEventsQuery {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("The period is not valid");
    }
}
