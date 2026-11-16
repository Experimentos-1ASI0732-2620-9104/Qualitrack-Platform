package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditLogRepository;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.assemblers.AuditLogEntryPersistenceAssembler;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.repositories.AuditLogEntryPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter for the {@link AuditLogRepository} port.
 * Translates between the {@link AuditLogEntry} domain model and the persistence entity
 * via {@link AuditLogEntryPersistenceAssembler}. No business rules live here:
 * this class only orchestrates queries and mappings.
 */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    // Discriminator constants used in ENTITY_TYPE/ENTITY_ID columns.
    // Kept as Strings to avoid coupling the persistence package to domain enums.
    private static final String EQUIPMENT_ENTITY_TYPE = "EQUIPMENT";
    private static final String BATCH_ENTITY_TYPE = "BATCH";

    private final AuditLogEntryPersistenceRepository persistenceRepository;

    public AuditLogRepositoryImpl(AuditLogEntryPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<AuditLogEntry> findById(Long id) {
        return persistenceRepository.findById(id)
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<AuditLogEntry> findAll() {
        return persistenceRepository.findAll().stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * Search by equipment: filters by (ENTITY_TYPE = EQUIPMENT, ENTITY_ID = equipmentId).
     * Backed by a derived query in the underlying Spring Data repository.
     */
    @Override
    public List<AuditLogEntry> findAllByEquipmentId(Long equipmentId) {
        return persistenceRepository.findAllByEntityTypeAndEntityId(EQUIPMENT_ENTITY_TYPE, equipmentId).stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }


    /**
     * Search by batch: filters by (ENTITY_TYPE = BATCH, ENTITY_ID = batchId).
     */
    @Override
    public List<AuditLogEntry> findAllByBatchId(Long batchId) {
        return persistenceRepository.findAllByEntityTypeAndEntityId(BATCH_ENTITY_TYPE, batchId).stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<AuditLogEntry> findAllByPerformedBy(Long performedBy) {
        return persistenceRepository.findAllByPerformedBy(performedBy).stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<AuditLogEntry> findAllByAction(AuditAction action) {
        return persistenceRepository.findAllByAction(action).stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * Search by date range over the timestamp stored as ISO-8601 String.
     * Relies on the lexicographic ordering of ISO-8601 so BETWEEN comparison is
     * equivalent to a temporal comparison.
     */
    @Override
    public List<AuditLogEntry> findAllByDateRange(String dateFrom, String dateTo) {
        return persistenceRepository.findAllByTimestampBetween(dateFrom, dateTo).stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Combined variant: EQUIPMENT entity + date range. */
    @Override
    public List<AuditLogEntry> findAllByEquipmentIdAndDateRange(Long equipmentId, String dateFrom, String dateTo) {
        return persistenceRepository
                .findAllByEntityTypeAndEntityIdAndTimestampBetween(
                        EQUIPMENT_ENTITY_TYPE,
                        equipmentId,
                        dateFrom,
                        dateTo
                )
                .stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /** Combined variant: BATCH entity + date range. */
    @Override
    public List<AuditLogEntry> findAllByBatchIdAndDateRange(Long batchId, String dateFrom, String dateTo) {
        return persistenceRepository
                .findAllByEntityTypeAndEntityIdAndTimestampBetween(
                        BATCH_ENTITY_TYPE,
                        batchId,
                        dateFrom,
                        dateTo
                )
                .stream()
                .map(AuditLogEntryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * Persists an entry: maps domain -> entity, saves and returns the entity mapped
     * back to domain (needed because JPA may assign id/timestamp on save).
     */
    @Override
    public AuditLogEntry save(AuditLogEntry auditLogEntry) {
        var entity = AuditLogEntryPersistenceAssembler.toPersistenceFromDomain(auditLogEntry);
        var savedEntity = persistenceRepository.save(entity);
        return AuditLogEntryPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public boolean existsById(Long id) {
        return persistenceRepository.existsById(id);
    }
}