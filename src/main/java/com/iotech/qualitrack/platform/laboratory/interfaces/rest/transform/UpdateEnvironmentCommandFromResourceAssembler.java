package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.UpdateEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.UpdateEnvironmentResource;

/**
 * Assembler to convert an UpdateEnvironmentResource to an UpdateEnvironmentCommand.
 */
public final class UpdateEnvironmentCommandFromResourceAssembler {

    private UpdateEnvironmentCommandFromResourceAssembler() {
    }

    public static UpdateEnvironmentCommand toCommandFromResource(Long laboratoryId, Long environmentId,
                                                                 UpdateEnvironmentResource resource) {
        return new UpdateEnvironmentCommand(laboratoryId, environmentId, resource.code(), resource.name(),
                resource.description());
    }
}
