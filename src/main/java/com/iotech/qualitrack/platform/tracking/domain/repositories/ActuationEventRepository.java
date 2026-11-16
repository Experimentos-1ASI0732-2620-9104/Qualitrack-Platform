package com.iotech.qualitrack.platform.tracking.domain.repositories;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository of the actions executed by the container monitors.
 */
public interface ActuationEventRepository {

    /**
     * Stores a new action.
     *
     * @return the stored action, with its identifier and reception time
     */
    ActuationEvent save(ActuationEvent event);

    /**
     * Actions of a device in a period, oldest first.
     */
    List<ActuationEvent> findByDeviceAndPeriod(Long deviceId, Instant from, Instant to);

    /**
     * Actions of every container monitor of an environment in a period, oldest first.
     */
    List<ActuationEvent> findByEnvironmentAndPeriod(Long laboratoryId, Long environmentId, Instant from, Instant to);

    /**
     * Action already received for the same device and moment, so a re-sent action is not stored twice.
     */
    Optional<ActuationEvent> findByDeviceAndActionAndOccurredAt(Long deviceId, String action, Instant occurredAt);

    /**
     * Moment the platform received the latest action of a device, used to know whether it is communicating.
     */
    Optional<Instant> findLastReceivedAt(Long deviceId);
}
