package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Monitored container where a product batch is stored (US79, TS69).
 */
public record GetBatchContainerQuery(Long laboratoryId, Long environmentId, Long productId, Long batchId) {
}
