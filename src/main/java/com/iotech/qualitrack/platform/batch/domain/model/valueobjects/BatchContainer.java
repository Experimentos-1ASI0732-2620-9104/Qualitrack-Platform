package com.iotech.qualitrack.platform.batch.domain.model.valueobjects;

import java.time.Instant;

/**
 * Read model of the monitored container where a product batch is stored (US79): the container and its environment.
 *
 * @param batchId            the product batch
 * @param containerMonitorId the container monitor that represents the container
 * @param containerName      the container monitor name, or null when it is no longer registered
 * @param environmentId      the environment of the container
 * @param assignedBy         user who stored the batch in the container
 * @param assignedAt         moment of the assignment
 */
public record BatchContainer(Long batchId, Long containerMonitorId, String containerName, Long environmentId,
                             Long assignedBy, Instant assignedAt) {
}
