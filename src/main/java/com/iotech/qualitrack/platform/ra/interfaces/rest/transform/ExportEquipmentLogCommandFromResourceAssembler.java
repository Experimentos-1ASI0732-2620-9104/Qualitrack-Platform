package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.commands.ExportEquipmentLogCommand;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.ExportEquipmentLogResource;

/**
 * Assembler that transforms equipment log REST resources into application commands.
 */
public final class ExportEquipmentLogCommandFromResourceAssembler {

    private ExportEquipmentLogCommandFromResourceAssembler() {
    }

    /**
     * Converts an equipment log export resource into a command.
     *
     * @param laboratoryId the laboratory from the request path
     * @param environmentId the environment of the equipment from the request path
     * @param equipmentId the equipment numeric identifier from the request path
     * @param resource the equipment log export request resource
     * @param requestedBy the authenticated user
     * @return the equipment log export command
     */
    public static ExportEquipmentLogCommand toCommandFromResource(
            Long laboratoryId,
            Long environmentId,
            Long equipmentId,
            ExportEquipmentLogResource resource,
            Long requestedBy
    ) {
        return new ExportEquipmentLogCommand(
                laboratoryId,
                environmentId,
                equipmentId,
                resource.startDate(),
                resource.endDate(),
                resource.format(),
                requestedBy
        );
    }
}