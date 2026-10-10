package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterEquipmentResource;

/**
 * Assembler to convert a RegisterEquipmentResource to a RegisterEquipmentCommand.
 */
public class RegisterEquipmentCommandFromResourceAssembler {

    /**
     * Converts a RegisterEquipmentResource to a RegisterEquipmentCommand.
     *
     * @param laboratoryId The laboratory of the request path.
     * @param resource The {@link RegisterEquipmentResource} resource to convert.
     * @return The {@link RegisterEquipmentCommand} command that results from the conversion.
     */
    public static RegisterEquipmentCommand toCommandFromResource(Long laboratoryId, RegisterEquipmentResource resource) {
        return new RegisterEquipmentCommand(
                laboratoryId,
                resource.name(),
                resource.type(),
                resource.model(),
                resource.serialNumber()
        );
    }
}