package com.iotech.qualitrack.platform.laboratory.domain.model.queries;

/**
 * Query to retrieve the environments registered in a laboratory.
 *
 * @param laboratoryId The laboratory whose environments are requested.
 */
public record GetEnvironmentsByLaboratoryIdQuery(Long laboratoryId) {
    public GetEnvironmentsByLaboratoryIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
    }
}
