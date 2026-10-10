package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalDeviationDetectedEvent;

import java.time.Instant;

/**
 * Integration event for Compliance &amp; Alerting: a metric of a device entered a worse condition.
 *
 * @param state          WARNING or CRITICAL
 * @param thresholdValue limit crossed by the reading
 */
public record EnvironmentalDeviationDetectedIntegrationEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                             Long deviceId, String metric, Double value, String unit,
                                                             String state, Double thresholdValue, Instant measuredAt) {
    public static EnvironmentalDeviationDetectedIntegrationEvent from(EnvironmentalDeviationDetectedEvent event) {
        return new EnvironmentalDeviationDetectedIntegrationEvent(event.measurementId(), event.laboratoryId(),
                event.environmentId(), event.deviceId(), event.metric(), event.value(), event.unit(), event.state(),
                event.thresholdValue(), event.measuredAt());
    }
}
