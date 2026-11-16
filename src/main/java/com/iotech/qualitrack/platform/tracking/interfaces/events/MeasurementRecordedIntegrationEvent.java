package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.MeasurementRecordedEvent;

import java.time.Instant;

/**
 * Integration event published when a reading of an IoT device is recorded.
 *
 * @param measurementId recorded reading
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the reading was taken
 * @param deviceId      device (equipment) that produced the reading
 * @param metric        name of the metric
 * @param value         numeric value, or null for an RFID reading
 * @param textValue     RFID tag read, or null for numeric metrics
 * @param unit          unit of the value
 * @param measuredAt    moment of the reading
 * @param state         NORMAL, WARNING or CRITICAL, or null when the metric has no threshold
 */
public record MeasurementRecordedIntegrationEvent(Long measurementId, Long laboratoryId, Long environmentId,
                                                  Long deviceId, String metric, Double value, String textValue,
                                                  String unit, Instant measuredAt, String state) {
    /**
     * Copies the domain event into the integration event shared with other contexts.
     */
    public static MeasurementRecordedIntegrationEvent from(MeasurementRecordedEvent event) {
        return new MeasurementRecordedIntegrationEvent(event.measurementId(), event.laboratoryId(), event.environmentId(),
                event.deviceId(), event.metric(), event.value(), event.textValue(), event.unit(), event.measuredAt(),
                event.state());
    }
}
