package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;

/**
 * Command to record the environment where an equipment or IoT device is located (US47, US52, US54).
 *
 * @param laboratoryId laboratory that owns the equipment and the environment
 * @param environmentId environment where the equipment is located
 * @param equipmentId equipment or IoT device to locate
 * @param requiredDeviceType device type the equipment must have, or null for any equipment
 */
public record AssignEquipmentToEnvironmentCommand(
        Long laboratoryId,
        Long environmentId,
        Long equipmentId,
        IotDeviceType requiredDeviceType
) {
    public AssignEquipmentToEnvironmentCommand {
        identifier(laboratoryId, "laboratoryId");
        identifier(environmentId, "environmentId");
        identifier(equipmentId, "equipmentId");
    }
}
