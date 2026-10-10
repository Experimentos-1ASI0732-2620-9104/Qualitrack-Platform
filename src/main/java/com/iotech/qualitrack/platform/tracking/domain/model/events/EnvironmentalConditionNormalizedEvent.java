package com.iotech.qualitrack.platform.tracking.domain.model.events;

import java.time.Instant;

/**
 * A reading brought a metric back to NORMAL after a WARNING or CRITICAL condition, so Compliance &amp; Alerting can
 * note it in the open alert of the incident.
 */
public record EnvironmentalConditionNormalizedEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                    Long deviceId, String metric, Double value, String unit,
                                                    Instant measuredAt) {
}
