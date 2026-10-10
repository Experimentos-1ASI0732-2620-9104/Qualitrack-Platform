package com.iotech.qualitrack.platform.ra.domain.model.queries;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;

import java.time.Instant;

/**
 * Query to get the indicators of a laboratory: operational counts and the summary of the environmental readings of a
 * period (US93, TS81).
 *
 * @param laboratoryId The numeric identifier of the laboratory. Cannot be null or less than 1.
 * @param environmentId Optional environment; null summarizes every environment of the laboratory.
 * @param period The period of the readings.
 */
public record GetKpiDashboardByLaboratoryIdQuery(Long laboratoryId, Long environmentId, ReportingPeriod period) {
    /**
     * Compact constructor for GetKpiDashboardByLaboratoryIdQuery.
     *
     * @throws IllegalArgumentException if laboratoryId is null or less than 1, or the period is missing.
     */
    public GetKpiDashboardByLaboratoryIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id is required and must be greater than 0.");
        }
        if (environmentId != null && environmentId <= 0) {
            throw new IllegalArgumentException("Environment id must be greater than 0.");
        }
        if (period == null) {
            throw new IllegalArgumentException("The period is required.");
        }
    }

    /**
     * Indicators of every environment for the default period ending now.
     *
     * @param laboratoryId The numeric identifier of the laboratory.
     */
    public GetKpiDashboardByLaboratoryIdQuery(Long laboratoryId) {
        this(laboratoryId, null, ReportingPeriod.forIndicators(null, null, Instant.now()));
    }
}
