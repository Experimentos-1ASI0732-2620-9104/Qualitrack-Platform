package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;

/**
 * Assembler to convert an Equipment entity to an EquipmentResource.
 */
public class EquipmentResourceFromEntityAssembler {

    /**
     * Converts an Equipment entity to an EquipmentResource.
     *
     * @param entity The {@link Equipment} entity to convert.
     * @return The {@link EquipmentResource} resource that results from the conversion.
     */
    public static EquipmentResource toResourceFromEntity(Equipment entity) {
        return new EquipmentResource(
                entity.getId(),
                entity.getLabId(),
                entity.getEnvironmentId(),
                entity.getName(),
                entity.getType().name(),
                entity.getModel(),
                entity.getSerialNumber(),
                entity.getStatus().name(),
                entity.getDeviceType() != null ? entity.getDeviceType().name() : null,
                entity.getSensorExternalId() != null ? entity.getSensorExternalId().value() : null,
                entity.getFirmwareVersion()
        );
    }
}