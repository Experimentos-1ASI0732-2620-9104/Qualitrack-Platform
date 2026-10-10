package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.required;

/**
 * Command to register a maintenance performed on an equipment of an environment (US49, TS35).
 *
 * @param laboratoryId laboratory that owns the equipment
 * @param environmentId environment where the equipment is located
 * @param equipmentId equipment that received the maintenance
 * @param maintenanceDate date of the intervention (YYYY-MM-DD)
 * @param technicianStaffId staff member of the laboratory who performed it
 * @param description work done, up to 1000 characters
 * @param type maintenance type (PREVENTIVE, CORRECTIVE, CALIBRATION, INSPECTION or OTHER)
 */
public record RegisterMaintenanceCommand(
        Long laboratoryId,
        Long environmentId,
        Long equipmentId,
        String maintenanceDate,
        Long technicianStaffId,
        String description,
        String type
) {
    public RegisterMaintenanceCommand {
        identifier(laboratoryId, "laboratoryId");
        identifier(environmentId, "environmentId");
        identifier(equipmentId, "equipmentId");
        maintenanceDate = required(maintenanceDate, "maintenanceDate", 10);
        identifier(technicianStaffId, "technicianStaffId");
        description = required(description, "description", 1000);
        type = required(type, "type", 50);
    }
}
