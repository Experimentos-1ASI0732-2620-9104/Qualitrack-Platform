package com.iotech.qualitrack.platform.ra.domain.model.queries;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;

/**
 * Query to get the deviation indicators of the variables of an environment in a period (US94, TS82).
 *
 * @param laboratoryId The laboratory of the environment.
 * @param environmentId The environment.
 * @param period The period of the readings.
 */
public record GetDeviationTrendsByEnvironmentQuery(Long laboratoryId, Long environmentId, ReportingPeriod period) {
    public GetDeviationTrendsByEnvironmentQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id is required and must be greater than 0.");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("Environment id is required and must be greater than 0.");
        }
        if (period == null) {
            throw new IllegalArgumentException("The period is required.");
        }
    }
}
