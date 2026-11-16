package com.iotech.qualitrack.platform.ra.application.queryservices;

import com.iotech.qualitrack.platform.ra.domain.model.queries.GetReportDocumentByIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportDocument;
import java.util.Optional;

/**
 * Application query service for retrieving stored report documents.
 */
public interface ReportDocumentQueryService {

    /**
     * Handles retrieval of the stored document of a report.
     *
     * @param query The query containing the report identifier.
     * @return The stored document, or empty when the report or its content is not found.
     */
    Optional<ReportDocument> handle(GetReportDocumentByIdQuery query);
}
