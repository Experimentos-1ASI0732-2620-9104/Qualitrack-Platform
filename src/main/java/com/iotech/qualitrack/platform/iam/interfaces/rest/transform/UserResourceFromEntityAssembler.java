package com.iotech.qualitrack.platform.iam.interfaces.rest.transform;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.UserResource;

/**
 * Assembler that maps IAM user aggregates to REST resources.
 */
public class UserResourceFromEntityAssembler {

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId(),
                user.getUsernameValue(),
                user.getEmailValue(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList(),
                user.getLaboratoryId(),
                user.getStatus().name()
        );
    }
}