package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.ContainerAssignmentResource;

public final class ContainerAssignmentResourceFromEntityAssembler {
    private ContainerAssignmentResourceFromEntityAssembler() { }

    public static ContainerAssignmentResource toResourceFromEntity(RawMaterialBatchContainer container) {
        return new ContainerAssignmentResource(container.receiptId(), container.containerMonitorId(), container.containerName(),
                container.environmentId(), container.assignedBy(), container.assignedAt());
    }
}
