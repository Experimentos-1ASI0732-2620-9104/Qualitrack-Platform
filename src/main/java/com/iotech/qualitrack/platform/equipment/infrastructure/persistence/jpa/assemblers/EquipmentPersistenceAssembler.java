package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;

/**
 * Static assembler between equipment domain and persistence representations.
 */
public final class EquipmentPersistenceAssembler {

    private EquipmentPersistenceAssembler() {
    }

    public static Equipment toDomainFromPersistence(EquipmentPersistenceEntity entity) {
        if (entity == null) return null;

        DeviceId deviceId = entity.getSensorExternalId() != null
                ? new DeviceId(entity.getSensorExternalId())
                : null;

        return new Equipment(
                entity.getId(),
                entity.getLabId(),
                entity.getEnvironmentId(),
                entity.getName(),
                new EquipmentType(entity.getType()),
                entity.getModel(),
                entity.getSerialNumber(),
                entity.getStatus(),
                deviceId,
                entity.getDeviceType(),
                entity.getFirmwareVersion()
        );
    }

    public static EquipmentPersistenceEntity toPersistenceFromDomain(Equipment equipment) {
        if (equipment == null) return null;

        var entity = new EquipmentPersistenceEntity();

        if (equipment.getId() != null) {
            entity.setId(equipment.getId());
        }

        entity.setLabId(equipment.getLabId());
        entity.setEnvironmentId(equipment.getEnvironmentId());
        entity.setName(equipment.getName());

        entity.setType(equipment.getType().name());

        entity.setModel(equipment.getModel());
        entity.setSerialNumber(equipment.getSerialNumber());
        entity.setStatus(equipment.getStatus());

        if (equipment.getSensorExternalId() != null) {
            entity.setSensorExternalId(equipment.getSensorExternalId().value());
        }
        entity.setDeviceType(equipment.getDeviceType());
        entity.setFirmwareVersion(equipment.getFirmwareVersion());

        return entity;
    }
}