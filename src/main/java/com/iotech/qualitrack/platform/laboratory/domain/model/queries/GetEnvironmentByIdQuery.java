package com.iotech.qualitrack.platform.laboratory.domain.model.queries;

/**
 * Query to retrieve one environment that belongs to a laboratory.
 *
 * @param laboratoryId The laboratory that must own the environment.
 * @param environmentId The requested environment.
 */
public record GetEnvironmentByIdQuery(Long laboratoryId, Long environmentId) {
    public GetEnvironmentByIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        }
    }
}
