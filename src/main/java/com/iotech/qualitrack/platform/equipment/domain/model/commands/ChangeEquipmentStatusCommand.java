package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.optional;

/**
 * Command to register a change in the operational status of an equipment (US48, TS34).
 *
 * @param laboratoryId laboratory that owns the equipment
 * @param environmentId environment where the equipment is located
 * @param equipmentId equipment whose status changes
 * @param status requested operational status
 * @param reason optional reason, up to 500 characters
 */
public record ChangeEquipmentStatusCommand(
        Long laboratoryId,
        Long environmentId,
        Long equipmentId,
        EquipmentStatus status,
        String reason
) {
    public ChangeEquipmentStatusCommand {
        identifier(laboratoryId, "laboratoryId");
        identifier(environmentId, "environmentId");
        identifier(equipmentId, "equipmentId");
        if (status == null) throw new IllegalArgumentException("status is required");
        reason = optional(reason, "reason", 500);
    }
}
