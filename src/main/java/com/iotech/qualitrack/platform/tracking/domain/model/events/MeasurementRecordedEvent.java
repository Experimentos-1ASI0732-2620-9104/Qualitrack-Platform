package com.iotech.qualitrack.platform.tracking.domain.model.events;

import java.time.Instant;

/**
 * A reading of an IoT device was recorded.
 *
 * @param state NORMAL, WARNING or CRITICAL, or null when the metric has no threshold
 */
public record MeasurementRecordedEvent(Long measurementId, Long laboratoryId, Long environmentId, Long deviceId,
                                       String metric, Double value, String textValue, String unit, Instant measuredAt,
                                       String state) {
}
