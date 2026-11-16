package com.iotech.qualitrack.platform.tracking.domain.model.events;

import java.time.Instant;

/**
 * A reading brought a metric back to NORMAL after a WARNING or CRITICAL condition, so Compliance &amp; Alerting can
 * note it in the open alert of the incident.
 *
 * @param measurementId reading that brought the metric back to NORMAL
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the reading was taken
 * @param deviceId      device (equipment) that produced the reading
 * @param metric        name of the metric
 * @param value         value of the reading
 * @param unit          unit of the value
 * @param measuredAt    moment of the reading
 */
public record EnvironmentalConditionNormalizedEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                    Long deviceId, String metric, Double value, String unit,
                                                    Instant measuredAt) {
}
