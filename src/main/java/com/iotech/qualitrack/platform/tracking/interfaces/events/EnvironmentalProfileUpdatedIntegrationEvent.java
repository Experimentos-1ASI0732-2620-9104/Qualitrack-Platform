package com.iotech.qualitrack.platform.tracking.interfaces.events;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalProfileUpdatedEvent;

/**
 * Integration event published when the thresholds or actuation rules of a profile change; Reporting &amp; Audit
 * records it in the audit trail.
 *
 * @param profileId     changed profile
 * @param laboratoryId  laboratory that owns the profile
 * @param scope         ENVIRONMENT or CONTAINER_MONITOR
 * @param environmentId configured environment, for the ENVIRONMENT scope
 * @param deviceId      configured container monitor, for the CONTAINER_MONITOR scope
 * @param version       new version of the profile
 * @param updatedBy     user that made the change
 */
public record EnvironmentalProfileUpdatedIntegrationEvent(Long profileId, Long laboratoryId, String scope,
                                                          Long environmentId, Long deviceId, long version, Long updatedBy) {
    /**
     * Copies the domain event into the integration event shared with other contexts.
     */
    public static EnvironmentalProfileUpdatedIntegrationEvent from(EnvironmentalProfileUpdatedEvent event) {
        return new EnvironmentalProfileUpdatedIntegrationEvent(event.profileId(), event.laboratoryId(), event.scope(),
                event.environmentId(), event.deviceId(), event.version(), event.updatedBy());
    }
}
