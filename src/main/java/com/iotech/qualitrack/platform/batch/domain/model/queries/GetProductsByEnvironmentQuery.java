package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Query for the pharmaceutical products registered in an environment (US72, TS62).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 */
public record GetProductsByEnvironmentQuery(Long laboratoryId, Long environmentId) {
}
