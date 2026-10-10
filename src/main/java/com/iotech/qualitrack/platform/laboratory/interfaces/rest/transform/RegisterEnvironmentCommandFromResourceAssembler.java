package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.CreateEnvironmentResource;

/**
 * Assembler to convert a CreateEnvironmentResource to a RegisterEnvironmentCommand.
 */
public final class RegisterEnvironmentCommandFromResourceAssembler {

    private RegisterEnvironmentCommandFromResourceAssembler() {
    }

    public static RegisterEnvironmentCommand toCommandFromResource(Long laboratoryId, CreateEnvironmentResource resource) {
        return new RegisterEnvironmentCommand(laboratoryId, resource.code(), resource.name(), resource.description());
    }
}
