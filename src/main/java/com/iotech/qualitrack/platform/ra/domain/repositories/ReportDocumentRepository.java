package com.iotech.qualitrack.platform.ra.domain.repositories;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportDocument;
import java.util.Optional;

/**
 * Domain repository port for the binary document of a report.
 *
 * <p>Manages only {@link ReportDocument} (id, format, raw bytes), which lives in
 * {@code audit_report_documents}, separate from the metadata handled by
 * {@code AuditReportRepository}. Content is immutable once written.
 */
public interface ReportDocumentRepository {

    /**
     * Persists the document. Implementations must reject overwriting an existing
     * report document (immutability) so downloads stay consistent with the report checksum.
     */
    void save(ReportDocument document);

    /**
     * Persists the document. Implementations must reject overwriting an existing
     * report document (immutability) so downloads stay consistent with the report checksum.
     */
    Optional<ReportDocument> findByReportId(Long reportId);
}
