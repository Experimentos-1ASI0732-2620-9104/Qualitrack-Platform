package com.iotech.qualitrack.platform.iam.interfaces.rest.transform;

import com.iotech.qualitrack.platform.iam.domain.model.commands.SignUpCommand;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.SignUpResource;

/**
 * Assembler that maps sign-up REST resources to domain commands.
 */
public class SignUpCommandFromResourceAssembler {

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(
                resource.username(),
                resource.email(),
                resource.password(),
                resource.roles(),
                resource.laboratoryId()
        );
    }
}