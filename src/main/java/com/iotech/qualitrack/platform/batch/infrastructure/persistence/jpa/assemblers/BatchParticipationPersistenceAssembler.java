package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.EquipmentUsagePersistenceEntity;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.StaffParticipationPersistenceEntity;

/**
 * Maps the equipment and staff associations of batches between the domain model and JPA.
 */
public final class BatchParticipationPersistenceAssembler {

    private BatchParticipationPersistenceAssembler() {
    }

    public static EquipmentUsage toDomainFromPersistence(EquipmentUsagePersistenceEntity entity) {
        return new EquipmentUsage(entity.getId(), entity.getBatchId(), entity.getEquipmentId(), entity.getEquipmentName(),
                entity.getRegisteredByUserId(), entity.getRegisteredAt());
    }

    public static StaffParticipation toDomainFromPersistence(StaffParticipationPersistenceEntity entity) {
        return new StaffParticipation(entity.getId(), entity.getBatchId(), entity.getStaffId(), entity.getStaffName(),
                entity.getStaffRole(), entity.getRegisteredByUserId(), entity.getRegisteredAt());
    }

    public static EquipmentUsagePersistenceEntity toPersistenceFromDomain(EquipmentUsage usage) {
        var entity = new EquipmentUsagePersistenceEntity();
        if (usage.getId() != null) entity.setId(usage.getId());
        entity.setBatchId(usage.getBatchId());
        entity.setEquipmentId(usage.getEquipmentId());
        entity.setEquipmentName(usage.getEquipmentName());
        entity.setRegisteredByUserId(usage.getRegisteredByUserId());
        entity.setRegisteredAt(usage.getRegisteredAt());
        return entity;
    }

    public static StaffParticipationPersistenceEntity toPersistenceFromDomain(StaffParticipation participation) {
        var entity = new StaffParticipationPersistenceEntity();
        if (participation.getId() != null) entity.setId(participation.getId());
        entity.setBatchId(participation.getBatchId());
        entity.setStaffId(participation.getStaffId());
        entity.setStaffName(participation.getStaffName());
        entity.setStaffRole(participation.getStaffRole());
        entity.setRegisteredByUserId(participation.getRegisteredByUserId());
        entity.setRegisteredAt(participation.getRegisteredAt());
        return entity;
    }
}
