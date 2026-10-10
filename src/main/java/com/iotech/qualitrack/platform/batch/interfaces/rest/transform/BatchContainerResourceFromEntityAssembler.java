package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchContainerResource;

/**
 * Maps the container of a product batch to its REST representation.
 */
public final class BatchContainerResourceFromEntityAssembler {
    private BatchContainerResourceFromEntityAssembler() {
    }

    public static BatchContainerResource toResourceFromEntity(BatchContainer container) {
        return new BatchContainerResource(container.batchId(), container.containerMonitorId(), container.containerName(),
                container.environmentId(), container.assignedBy(), container.assignedAt());
    }
}
