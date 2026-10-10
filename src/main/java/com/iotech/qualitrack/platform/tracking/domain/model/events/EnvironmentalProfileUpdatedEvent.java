package com.iotech.qualitrack.platform.tracking.domain.model.events;

/**
 * The thresholds or actuation rules of an environmental profile changed.
 *
 * @param scope ENVIRONMENT or CONTAINER_MONITOR
 */
public record EnvironmentalProfileUpdatedEvent(Long profileId, Long laboratoryId, String scope, Long environmentId,
                                               Long deviceId, long version, Long updatedBy) {
}
