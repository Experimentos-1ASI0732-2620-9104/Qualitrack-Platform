package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalConditionNormalizedEvent;

import java.time.Instant;

/**
 * Integration event for Compliance &amp; Alerting: a metric of a device returned to NORMAL after a deviation.
 */
public record EnvironmentalConditionNormalizedIntegrationEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                               Long deviceId, String metric, Double value, String unit,
                                                               Instant measuredAt) {
    public static EnvironmentalConditionNormalizedIntegrationEvent from(EnvironmentalConditionNormalizedEvent event) {
        return new EnvironmentalConditionNormalizedIntegrationEvent(event.measurementId(), event.laboratoryId(),
                event.environmentId(), event.deviceId(), event.metric(), event.value(), event.unit(), event.measuredAt());
    }
}
