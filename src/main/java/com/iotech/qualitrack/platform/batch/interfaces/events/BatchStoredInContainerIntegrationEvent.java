package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStoredInContainerEvent;

/**
 * Integration event published when a product batch is stored in a monitored container.
 */
public record BatchStoredInContainerIntegrationEvent(Long batchId, Long laboratoryId, Long containerMonitorId,
                                                     String containerName, Long environmentId) {
    public static BatchStoredInContainerIntegrationEvent from(BatchStoredInContainerEvent event) {
        return new BatchStoredInContainerIntegrationEvent(event.batchId(), event.laboratoryId(), event.containerMonitorId(),
                event.containerName(), event.environmentId());
    }
}
