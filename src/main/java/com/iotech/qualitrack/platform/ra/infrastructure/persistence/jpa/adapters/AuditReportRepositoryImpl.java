package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportType;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditReportRepository;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.assemblers.AuditReportPersistenceAssembler;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.repositories.AuditReportPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter for the {@link AuditReportRepository} port.
 * Exposes queries by laboratory, batch, equipment and report type, plus saving.
 * All domain <-> persistence conversion is delegated to {@link AuditReportPersistenceAssembler}
 * to keep this adapter free of mapping logic.
 */
@Repository
public class AuditReportRepositoryImpl implements AuditReportRepository {

    private final AuditReportPersistenceRepository persistenceRepository;

    public AuditReportRepositoryImpl(AuditReportPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<AuditReport> findById(Long id) {
        return persistenceRepository.findById(id)
                .map(AuditReportPersistenceAssembler::toDomainFromPersistence);
    }

    /** Lists reports generated for a laboratory (source for the laboratory "Reports" view). */
    @Override
    public List<AuditReport> findAllByLaboratoryId(Long laboratoryId) {
        return persistenceRepository.findAllByLaboratoryId(laboratoryId).stream()
                .map(AuditReportPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Lists reports attached to a specific batch (batch traceability). */
    @Override
    public List<AuditReport> findAllByBatchId(Long batchId) {
        return persistenceRepository.findAllByBatchId(batchId).stream()
                .map(AuditReportPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Lists reports attached to an equipment (activity audit). */
    @Override
    public List<AuditReport> findAllByEquipmentId(Long equipmentId) {
        return persistenceRepository.findAllByEquipmentId(equipmentId).stream()
                .map(AuditReportPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Lists reports of a functional type (e.g. INVENTORY, COMPLIANCE, EQUIPMENT_LOG). */
    @Override
    public List<AuditReport> findAllByReportType(ReportType reportType) {
        return persistenceRepository.findAllByReportType(reportType).stream()
                .map(AuditReportPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Saves or updates report metadata; binary content lives in {@code ReportDocumentRepository}. */
    @Override
    public AuditReport save(AuditReport auditReport) {
        var entity = AuditReportPersistenceAssembler.toPersistenceFromDomain(auditReport);
        var savedEntity = persistenceRepository.save(entity);
        return AuditReportPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public boolean existsById(Long id) {
        return persistenceRepository.existsById(id);
    }
}