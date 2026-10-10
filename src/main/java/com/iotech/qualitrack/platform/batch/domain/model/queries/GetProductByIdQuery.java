package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Query for one pharmaceutical product of an environment.
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 */
public record GetProductByIdQuery(Long laboratoryId, Long environmentId, Long productId) {
}
