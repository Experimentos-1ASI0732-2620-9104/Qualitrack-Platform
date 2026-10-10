package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateProductCommand;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.CreateProductResource;

/**
 * Builds the product registration command from the request body and the path.
 */
public final class CreateProductCommandFromResourceAssembler {

    private CreateProductCommandFromResourceAssembler() {
    }

    public static CreateProductCommand toCommandFromResource(Long laboratoryId, Long environmentId, CreateProductResource resource) {
        return new CreateProductCommand(laboratoryId, environmentId, resource.code(), resource.name(),
                resource.description(), resource.specifications());
    }
}
