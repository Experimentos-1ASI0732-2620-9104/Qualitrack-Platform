package com.iotech.qualitrack.platform.iam.interfaces.rest.transform;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;

/**
 * Assembler that maps authenticated user results to REST resources.
 */
public class AuthenticatedUserResourceFromEntityAssembler {

    public static AuthenticatedUserResource toResourceFromEntity(
            UserCommandService.AuthenticatedUser authenticatedUser
    ) {
        var user = authenticatedUser.user();

        return new AuthenticatedUserResource(
                user.getId(),
                user.getUsernameValue(),
                authenticatedUser.token(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList(),
                user.getLaboratoryId(),
                user.isPasswordChangeRequired()
        );
    }
}