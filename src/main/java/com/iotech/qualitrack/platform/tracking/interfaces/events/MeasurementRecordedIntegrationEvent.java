package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.MeasurementRecordedEvent;

import java.time.Instant;

/**
 * Integration event published when a reading of an IoT device is recorded.
 */
public record MeasurementRecordedIntegrationEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                  Long deviceId, String metric, Double value, String textValue,
                                                  String unit, Instant measuredAt, String state) {
    public static MeasurementRecordedIntegrationEvent from(MeasurementRecordedEvent event) {
        return new MeasurementRecordedIntegrationEvent(event.measurementId(), event.laboratoryId(), event.environmentId(),
                event.deviceId(), event.metric(), event.value(), event.textValue(), event.unit(), event.measuredAt(),
                event.state());
    }
}
