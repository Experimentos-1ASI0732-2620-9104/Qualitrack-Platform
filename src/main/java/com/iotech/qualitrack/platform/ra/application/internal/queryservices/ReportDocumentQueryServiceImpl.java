package com.iotech.qualitrack.platform.ra.application.internal.queryservices;

import com.iotech.qualitrack.platform.ra.application.queryservices.ReportDocumentQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetReportDocumentByIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportDocument;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditReportRepository;
import com.iotech.qualitrack.platform.ra.domain.repositories.ReportDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Application service implementation that retrieves stored report documents.
 */
@Service
@Transactional(readOnly = true)
public class ReportDocumentQueryServiceImpl implements ReportDocumentQueryService {
    private final AuditReportRepository reports;
    private final ReportDocumentRepository documents;

    public ReportDocumentQueryServiceImpl(AuditReportRepository reports, ReportDocumentRepository documents) {
        this.reports = reports;
        this.documents = documents;
    }

    /**
     * Handles retrieval of the stored document of a report, after verifying its checksum.
     *
     * @param query The query containing the report identifier.
     * @return The stored document, or empty when the report or its content is not found.
     * @throws IllegalStateException When the stored content does not match the report checksum.
     */
    @Override
    public Optional<ReportDocument> handle(GetReportDocumentByIdQuery query) {
        return reports.findById(query.reportId()).flatMap(report -> documents.findByReportId(query.reportId())
                .map(document -> {
                    if (!report.getChecksum().equals(report.computeChecksum(document.content()))) {
                        throw new IllegalStateException("Stored report checksum mismatch");
                    }
                    return document;
                }));
    }
}
