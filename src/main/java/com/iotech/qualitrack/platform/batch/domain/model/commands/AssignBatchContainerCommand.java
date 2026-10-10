package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Stores a product batch in a monitored container of a product storage environment (US78, TS68).
 *
 * @param laboratoryId       the laboratory of the batch
 * @param environmentId      the environment of the product
 * @param productId          the product of the batch
 * @param batchId            the batch
 * @param containerMonitorId the container monitor that represents the container
 */
public record AssignBatchContainerCommand(Long laboratoryId, Long environmentId, Long productId, Long batchId,
                                          Long containerMonitorId) {
}
