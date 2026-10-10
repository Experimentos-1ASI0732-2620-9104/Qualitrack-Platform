package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.CreateBatchResource;

/**
 * Builds the batch registration command from the path hierarchy and the request body.
 */
public final class CreateBatchCommandFromResourceAssembler {

    private CreateBatchCommandFromResourceAssembler() {
    }

    public static CreateBatchCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long productId,
                                                           CreateBatchResource resource) {
        return new CreateBatchCommand(laboratoryId, environmentId, productId, resource.batchNumber(), resource.quantity(),
                resource.unit(), resource.startDate(), resource.notes());
    }
}
