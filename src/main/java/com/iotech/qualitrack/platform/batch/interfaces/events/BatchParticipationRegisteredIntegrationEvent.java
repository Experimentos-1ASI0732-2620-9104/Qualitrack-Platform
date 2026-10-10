package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchParticipationRegisteredEvent;

/**
 * Integration event published when an equipment or a staff member is associated with a product batch.
 */
public record BatchParticipationRegisteredIntegrationEvent(Long batchId, Long laboratoryId, String resourceType,
                                                           Long resourceId, String resourceName) {
    public static BatchParticipationRegisteredIntegrationEvent from(BatchParticipationRegisteredEvent event) {
        return new BatchParticipationRegisteredIntegrationEvent(event.batchId(), event.laboratoryId(), event.resourceType(),
                event.resourceId(), event.resourceName());
    }
}
