package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalProfileUpdatedEvent;

/**
 * Integration event published when the thresholds or actuation rules of a profile change.
 */
public record EnvironmentalProfileUpdatedIntegrationEvent(Long profileId, Long laboratoryId, String scope,
                                                          Long environmentId, Long deviceId, long version, Long updatedBy) {
    public static EnvironmentalProfileUpdatedIntegrationEvent from(EnvironmentalProfileUpdatedEvent event) {
        return new EnvironmentalProfileUpdatedIntegrationEvent(event.profileId(), event.laboratoryId(), event.scope(),
                event.environmentId(), event.deviceId(), event.version(), event.updatedBy());
    }
}
