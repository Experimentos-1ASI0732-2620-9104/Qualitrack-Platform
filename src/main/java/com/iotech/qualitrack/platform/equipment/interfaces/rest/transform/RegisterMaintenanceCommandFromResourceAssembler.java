package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterMaintenanceCommand;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterMaintenanceResource;

/**
 * Assembler to convert a RegisterMaintenanceResource to a RegisterMaintenanceCommand.
 */
public class RegisterMaintenanceCommandFromResourceAssembler {

    /**
     * Converts a RegisterMaintenanceResource to a RegisterMaintenanceCommand.
     *
     * @param equipmentId The equipment numeric ID extracted from the path.
     * @param resource The {@link RegisterMaintenanceResource} resource to convert.
     * @return The {@link RegisterMaintenanceCommand} command that results from the conversion.
     */
    public static RegisterMaintenanceCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long equipmentId,
                                                                   RegisterMaintenanceResource resource) {
        return new RegisterMaintenanceCommand(
                laboratoryId,
                environmentId,
                equipmentId,
                resource.maintenanceDate(),
                resource.technicianStaffId(),
                resource.description(),
                resource.type()
        );
    }
}