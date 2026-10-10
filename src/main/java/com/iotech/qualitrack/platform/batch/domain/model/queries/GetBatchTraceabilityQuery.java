package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Query for the traceability of a product batch (US80, TS70).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 * @param batchId the batch identifier
 */
public record GetBatchTraceabilityQuery(Long laboratoryId, Long environmentId, Long productId, Long batchId) {
}
