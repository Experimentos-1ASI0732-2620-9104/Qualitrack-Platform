package com.iotech.qualitrack.platform.laboratory.application.queryservices;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentsByLaboratoryIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for laboratory environment read queries.
 */
public interface EnvironmentQueryService {

    /**
     * Handles retrieval of the environments registered in a laboratory.
     *
     * @param query laboratory-id query
     * @return environments of the laboratory ordered by code
     */
    List<Environment> handle(GetEnvironmentsByLaboratoryIdQuery query);

    /**
     * Handles retrieval of one environment of a laboratory.
     *
     * @param query laboratory and environment identifiers
     * @return the environment when it exists in the laboratory
     */
    Optional<Environment> handle(GetEnvironmentByIdQuery query);
}
