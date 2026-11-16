package com.iotech.qualitrack.platform.tracking.domain.model.events;

import java.time.Instant;

/**
 * A reading of an IoT device was recorded. It is published for every new reading, whatever its state; readings
 * re-sent by the Edge do not publish it again.
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
public record MeasurementRecordedEvent(Long measurementId, Long laboratoryId, Long environmentId, Long deviceId,
                                       String metric, Double value, String textValue, String unit, Instant measuredAt,
                                       String state) {
}
