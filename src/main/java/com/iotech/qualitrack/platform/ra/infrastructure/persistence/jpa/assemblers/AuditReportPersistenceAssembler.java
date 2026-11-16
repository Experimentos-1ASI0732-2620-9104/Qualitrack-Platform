package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities.AuditReportPersistenceEntity;

/**
 * Static assembler between {@link AuditReport} (domain) and
 * {@link AuditReportPersistenceEntity} (persistence).
 * Follows the same contract as other assemblers in the module: 1:1 mapping, no rules,
 * so any schema change is reflected in a single place.
 */

public final class AuditReportPersistenceAssembler {

    private AuditReportPersistenceAssembler() {
    }

    /** JPA entity -> domain aggregate. */
    public static AuditReport toDomainFromPersistence(AuditReportPersistenceEntity entity) {
        if (entity == null) return null;

        return new AuditReport(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getBatchId(),
                entity.getEquipmentId(),
                entity.getGeneratedBy(),
                entity.getGeneratedByName(),
                entity.getReportType(),
                entity.getDateRangeFrom(),
                entity.getDateRangeTo(),
                entity.getFilePath(),
                entity.getChecksum(),
                entity.getGeneratedAt()
        );
    }

    /**
     * Domain aggregate -> JPA entity.
     * The id is only copied when it already exists (update); on inserts it is left null.
     */

    public static AuditReportPersistenceEntity toPersistenceFromDomain(AuditReport report) {
        if (report == null) return null;

        var entity = new AuditReportPersistenceEntity();

        if (report.getId() != null) {
            entity.setId(report.getId());
        }

        entity.setLaboratoryId(report.getLaboratoryId());
        entity.setBatchId(report.getBatchId());
        entity.setEquipmentId(report.getEquipmentId());
        entity.setGeneratedBy(report.getGeneratedBy());
        entity.setGeneratedByName(report.getGeneratedByName());
        entity.setReportType(report.getReportType());
        entity.setDateRangeFrom(report.getDateRangeFrom());
        entity.setDateRangeTo(report.getDateRangeTo());
        entity.setFilePath(report.getFilePath());
        entity.setChecksum(report.getChecksum());
        entity.setGeneratedAt(report.getGeneratedAt());

        return entity;
    }
}