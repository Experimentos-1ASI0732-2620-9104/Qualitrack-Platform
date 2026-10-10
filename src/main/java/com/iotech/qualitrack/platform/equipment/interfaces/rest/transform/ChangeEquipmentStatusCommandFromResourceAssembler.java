package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.ChangeEquipmentStatusCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.ChangeEquipmentStatusResource;

import java.util.Arrays;
import java.util.Locale;

/**
 * Assembler to convert a ChangeEquipmentStatusResource into a ChangeEquipmentStatusCommand.
 */
public class ChangeEquipmentStatusCommandFromResourceAssembler {

    /**
     * @throws IllegalArgumentException when the status is not one of the allowed operational statuses
     */
    public static ChangeEquipmentStatusCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long equipmentId,
                                                                     ChangeEquipmentStatusResource resource) {
        var requested = resource.status().trim().toUpperCase(Locale.ROOT);
        var status = Arrays.stream(EquipmentStatus.values()).filter(value -> value.name().equals(requested)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Status must be one of " + Arrays.toString(EquipmentStatus.values())));
        return new ChangeEquipmentStatusCommand(laboratoryId, environmentId, equipmentId, status, resource.reason());
    }
}
