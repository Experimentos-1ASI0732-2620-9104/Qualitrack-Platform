package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Query for one batch of a product, addressed through its full hierarchy (US74).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 * @param batchId the batch identifier
 */
public record GetProductBatchQuery(Long laboratoryId, Long environmentId, Long productId, Long batchId) {
}
