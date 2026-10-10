package com.iotech.qualitrack.platform.tracking.domain.repositories;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository of the readings produced by the IoT devices.
 */
public interface MeasurementRepository {
    Measurement save(Measurement measurement);

    /**
     * Readings of a device in a period, oldest first.
     *
     * @param metric metric name to keep, or null for every metric
     */
    List<Measurement> findByDeviceAndPeriod(Long deviceId, String metric, Instant from, Instant to);

    /**
     * Reading already received for the same device, metric and moment, so a re-sent reading is not stored twice.
     */
    Optional<Measurement> findByDeviceAndMetricAndMeasuredAt(Long deviceId, String metric, Instant measuredAt);

    /**
     * Latest reading of a device and metric taken before a moment, used to know the previous condition.
     */
    Optional<Measurement> findPreviousReading(Long deviceId, String metric, Instant before);

    /**
     * Readings of every device of an environment in a period, oldest first, used by Reporting &amp; Audit.
     */
    List<Measurement> findByEnvironmentAndPeriod(Long laboratoryId, Long environmentId, Instant from, Instant to);

    Optional<Instant> findLastReceivedAt(Long deviceId);
}
