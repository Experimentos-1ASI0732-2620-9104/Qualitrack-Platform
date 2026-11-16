package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportType;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity holding generated report metadata.
 * Extends {@link AuditableAbstractPersistenceEntity} to reuse common audit fields
 * (creation/update) from the shared module.
 * Binary content does not live here: it is separated into {@link ReportDocumentPersistenceEntity}.
 */
@Entity
@Table(name = "audit_reports")
@Getter
@Setter
@NoArgsConstructor
public class AuditReportPersistenceEntity extends AuditableAbstractPersistenceEntity {

    /** Owning laboratory of the report (used by the tenant filter). */
    @Column(name = "laboratory_id", nullable = false)
    private Long laboratoryId;

    /** batch the report is about. */
    @Column(name = "batch_id")
    private Long batchId;

    /** equipment the report is about. */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** Id of the user who requested generation. */
    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    /** User name at generation time (snapshot, not a join). */
    @Column(name = "generated_by_name", nullable = false, length = 150)
    private String generatedByName;

    /** Functional report type; converted automatically by {@code ReportTypePersistenceConverter}. */
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    /** Period start covered by the report (ISO-8601), optional for period-less reports. */
    @Column(name = "date_range_from", length = 30)
    private String dateRangeFrom;

    /** Period end covered by the report (ISO-8601), optional. */
    @Column(name = "date_range_to", length = 30)
    private String dateRangeTo;

    /**
     * Logical path of the report (not necessarily a filesystem path: may be a storage key).
     * Kept for compatibility with legacy reports.
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * Hash (e.g. SHA-256) of the binary content, used to verify integrity on download
     * and to detect tampering in {@link ReportDocumentPersistenceEntity}.
     */
    @Column(nullable = false, length = 255)
    private String checksum;

    /** Generation instant (ISO-8601 as String for portability across engines). */
    @Column(name = "generated_at", nullable = false, length = 30)
    private String generatedAt;
}