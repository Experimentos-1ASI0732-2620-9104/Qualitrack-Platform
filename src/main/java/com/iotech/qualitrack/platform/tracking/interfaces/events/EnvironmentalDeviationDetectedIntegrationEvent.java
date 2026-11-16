package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalDeviationDetectedEvent;

import java.time.Instant;

/**
 * Integration event for Compliance &amp; Alerting: a metric of a device entered a worse condition.
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
public record EnvironmentalDeviationDetectedIntegrationEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                             Long deviceId, String metric, Double value, String unit,
                                                             String state, Double thresholdValue, Instant measuredAt) {
    /**
     * Copies the domain event into the integration event shared with other contexts.
     */
    public static EnvironmentalDeviationDetectedIntegrationEvent from(EnvironmentalDeviationDetectedEvent event) {
        return new EnvironmentalDeviationDetectedIntegrationEvent(event.measurementId(), event.laboratoryId(),
                event.environmentId(), event.deviceId(), event.metric(), event.value(), event.unit(), event.state(),
                event.thresholdValue(), event.measuredAt());
    }
}
