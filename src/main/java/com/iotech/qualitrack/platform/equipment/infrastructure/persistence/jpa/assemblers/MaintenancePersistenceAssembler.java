package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.MaintenancePersistenceEntity;

/**
 * Static assembler between maintenance record domain and persistence representations.
 */
public final class MaintenancePersistenceAssembler {

    private MaintenancePersistenceAssembler() {
    }

    public static MaintenanceRecord toDomainFromPersistence(MaintenancePersistenceEntity entity) {
        if (entity == null) return null;

        return new MaintenanceRecord(
                entity.getId(),
                entity.getEquipmentId(),
                entity.getEnvironmentId(),
                entity.getMaintenanceDate(),
                entity.getTechnicianName(),
                entity.getTechnicianStaffId(),
                entity.getDescription(),
                entity.getType()
        );
    }

    public static MaintenancePersistenceEntity toPersistenceFromDomain(MaintenanceRecord record) {
        if (record == null) return null;

        var entity = new MaintenancePersistenceEntity();

        if (record.getId() != null) {
            entity.setId(record.getId());
        }

        entity.setEquipmentId(record.getEquipmentId());
        entity.setEnvironmentId(record.getEnvironmentId());
        entity.setMaintenanceDate(record.getMaintenanceDate());
        entity.setTechnicianName(record.getTechnicianName());
        entity.setTechnicianStaffId(record.getTechnicianStaffId());
        entity.setDescription(record.getDescription());
        entity.setType(record.getType());

        return entity;
    }
}