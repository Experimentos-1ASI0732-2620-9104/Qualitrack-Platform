package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Query for the batches manufactured of a product (US74, TS64).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 */
public record GetBatchesByProductQuery(Long laboratoryId, Long environmentId, Long productId) {
}
