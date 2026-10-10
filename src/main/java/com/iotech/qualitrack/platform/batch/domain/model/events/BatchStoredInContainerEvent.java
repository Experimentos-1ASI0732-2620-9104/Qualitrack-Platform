package com.iotech.qualitrack.platform.batch.domain.model.events;

/**
 * Domain event published when a product batch is stored in a monitored container.
 */
public record BatchStoredInContainerEvent(Long batchId, Long laboratoryId, Long containerMonitorId, String containerName,
                                          Long environmentId) {
}
