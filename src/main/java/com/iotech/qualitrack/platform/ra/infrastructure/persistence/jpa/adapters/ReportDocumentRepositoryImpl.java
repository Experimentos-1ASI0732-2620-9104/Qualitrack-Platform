package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportDocument;
import com.iotech.qualitrack.platform.ra.domain.repositories.ReportDocumentRepository;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities.ReportDocumentPersistenceEntity;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.repositories.ReportDocumentPersistenceRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * JPA adapter for the {@link ReportDocumentRepository} port.
 * Manages exclusively the report binary content (PDF/CSV) stored in
 * {@code audit_report_documents}, separated from metadata in {@code audit_reports}.
 * Content is immutable: {@link #save} rejects overwriting an existing reportId.
 */
@Repository
public class ReportDocumentRepositoryImpl implements ReportDocumentRepository {
    private final ReportDocumentPersistenceRepository repository;

    public ReportDocumentRepositoryImpl(ReportDocumentPersistenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists the binary content of a report.
     * If a document already exists for {@code reportId}, throws {@link IllegalStateException}:
     * immutability guarantees downloads match the checksum recorded in
     * {@link com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport}.
     */
    @Override
    public void save(ReportDocument document) {
        if (repository.existsById(document.reportId())) throw new IllegalStateException("Report content is immutable");
        var entity = new ReportDocumentPersistenceEntity();
        entity.setReportId(document.reportId());
        entity.setFormat(document.format());
        entity.setContent(document.content());
        repository.save(entity);
    }

    /**
     * Retrieves the binary content by report id. Used on download
     * ({@code GET /api/v1/reports/{id}/content}) to serve the original PDF/CSV.
     */
    @Override
    public Optional<ReportDocument> findByReportId(Long reportId) {
        return repository.findById(reportId)
                .map(entity -> new ReportDocument(entity.getReportId(), entity.getFormat(), entity.getContent()));
    }
}
