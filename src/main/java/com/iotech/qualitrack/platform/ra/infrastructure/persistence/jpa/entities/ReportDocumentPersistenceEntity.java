package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity storing the binary content of a generated report.
 * It is modeled separately from {@link AuditReportPersistenceEntity} because:
 * <ul>
 *   <li>The binary (PDF/CSV) can be large and is streamed on download.</li>
 *   <li>Report metadata is queried frequently without needing the binary.</li>
 *   <li>Content is immutable once generated (see {@code ReportDocumentRepositoryImpl}).</li>
 * </ul>
 */
@Entity
@Table(name = "audit_report_documents")
@Getter
@Setter
@NoArgsConstructor
public class ReportDocumentPersistenceEntity {
    /** PK shared with {@code audit_reports.id}; 1:1 logical relation by id. */
    @Id
    @Column(name = "report_id")
    private Long reportId;

    /** Content format (PDF/CSV). Converted automatically by {@code ReportFormatPersistenceConverter}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportFormat format;

    /**
     * Report binary content.
     * {@code LONGBLOB} supports up to 4 GB on MySQL/MariaDB, enough for large reports.
     * Annotated {@code @Lob} so Hibernate treats it as a large binary.
     */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;
}
