package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.commands.AssignRawMaterialBatchContainerCommand;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.AssignContainerResource;

public final class AssignRawMaterialBatchContainerCommandFromResourceAssembler {
    private AssignRawMaterialBatchContainerCommandFromResourceAssembler() { }

    /**
     * @throws IllegalArgumentException when the container monitor is missing (400)
     */
    public static AssignRawMaterialBatchContainerCommand toCommandFromResource(Long laboratoryId, Long environmentId,
            Long rawMaterialId, Long rawMaterialBatchId, AssignContainerResource resource) {
        if (resource == null || resource.containerMonitorId() == null) {
            throw new IllegalArgumentException("Container monitor is required");
        }
        return new AssignRawMaterialBatchContainerCommand(laboratoryId, environmentId, rawMaterialId, rawMaterialBatchId,
                resource.containerMonitorId());
    }
}
