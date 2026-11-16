package com.iotech.qualitrack.platform.tracking.domain.model.events;

import java.time.Instant;

/**
 * A reading moved a metric to a worse condition (NORMAL to WARNING, NORMAL or WARNING to CRITICAL), so Compliance
 * &amp; Alerting can create the alert of the incident.
 *
 * @param measurementId  reading that worsened the condition
 * @param laboratoryId   laboratory that owns the device
 * @param environmentId  environment where the reading was taken
 * @param deviceId       device (equipment) that produced the reading
 * @param metric         name of the metric
 * @param value          value of the reading
 * @param unit           unit of the value
 * @param state          WARNING or CRITICAL
 * @param thresholdValue limit crossed by the reading
 * @param measuredAt     moment of the reading
 */
public record EnvironmentalDeviationDetectedEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                  Long deviceId, String metric, Double value, String unit, String state,
                                                  Double thresholdValue, Instant measuredAt) {
}
