package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.required;

/**
 * Command to register an equipment in a laboratory (US45, TS31).
 *
 * @param labId The numeric identifier of the laboratory. Cannot be null or less than 1.
 * @param name The display name of the equipment, up to 150 characters.
 * @param type The category of the equipment, up to 100 characters.
 * @param model The model of the equipment, up to 100 characters.
 * @param serialNumber The serial number for traceability, up to 50 characters.
 */
public record RegisterEquipmentCommand(
        Long labId,
        String name,
        String type,
        String model,
        String serialNumber
) {
    /**
     * Compact constructor for RegisterEquipmentCommand.
     * Enforces Fail-Fast validation.
     */
    public RegisterEquipmentCommand {
        identifier(labId, "labId");
        name = required(name, "name", 150);
        type = required(type, "type", 100);
        model = required(model, "model", 100);
        serialNumber = required(serialNumber, "serialNumber", 50);
    }
}
