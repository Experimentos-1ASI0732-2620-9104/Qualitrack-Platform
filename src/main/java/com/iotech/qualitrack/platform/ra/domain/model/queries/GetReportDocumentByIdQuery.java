package com.iotech.qualitrack.platform.ra.domain.model.queries;

/**
 * Query requesting the stored document (binary content) of a report by id.
 *
 * <p>Immutable CQRS query: only carries the identifier and performs fail-fast validation.
 * Existence, tenancy and format checks belong to the query service.
 *
 * @param reportId identifier of the report; must be positive
 */
public record GetReportDocumentByIdQuery(Long reportId) {
    public GetReportDocumentByIdQuery {
        if (reportId == null || reportId <= 0) throw new IllegalArgumentException("A report ID is required");
    }
}
