package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchParticipationRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers.BatchParticipationPersistenceAssembler;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.EquipmentUsagePersistenceRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.StaffParticipationPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA adapter of the batch participation repository.
 */
@Repository
public class BatchParticipationRepositoryImpl implements BatchParticipationRepository {

    private final EquipmentUsagePersistenceRepository equipmentUsages;
    private final StaffParticipationPersistenceRepository staffParticipations;

    public BatchParticipationRepositoryImpl(EquipmentUsagePersistenceRepository equipmentUsages,
                                            StaffParticipationPersistenceRepository staffParticipations) {
        this.equipmentUsages = equipmentUsages;
        this.staffParticipations = staffParticipations;
    }

    @Override
    public EquipmentUsage saveEquipmentUsage(EquipmentUsage usage) {
        return BatchParticipationPersistenceAssembler.toDomainFromPersistence(
                equipmentUsages.save(BatchParticipationPersistenceAssembler.toPersistenceFromDomain(usage)));
    }

    @Override
    public StaffParticipation saveStaffParticipation(StaffParticipation participation) {
        return BatchParticipationPersistenceAssembler.toDomainFromPersistence(
                staffParticipations.save(BatchParticipationPersistenceAssembler.toPersistenceFromDomain(participation)));
    }

    @Override
    public boolean existsEquipmentUsage(Long batchId, Long equipmentId) {
        return equipmentUsages.existsByBatchIdAndEquipmentId(batchId, equipmentId);
    }

    @Override
    public boolean existsStaffParticipation(Long batchId, Long staffId) {
        return staffParticipations.existsByBatchIdAndStaffId(batchId, staffId);
    }

    @Override
    public List<EquipmentUsage> findEquipmentUsagesByBatchId(Long batchId) {
        return equipmentUsages.findAllByBatchIdOrderByIdAsc(batchId).stream()
                .map(BatchParticipationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<StaffParticipation> findStaffParticipationsByBatchId(Long batchId) {
        return staffParticipations.findAllByBatchIdOrderByIdAsc(batchId).stream()
                .map(BatchParticipationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
