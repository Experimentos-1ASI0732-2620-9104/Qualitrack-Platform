package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import java.util.List;

/**
 * Resource representing an IAM user.
 *
 * @param id user identifier
 * @param username account username
 * @param roles assigned roles
 * @param laboratoryId associated laboratory identifier
 * @param status user status
 */
public record UserResource(
        Long id,
        String username,
        String email,
        List<String> roles,
        Long laboratoryId,
        String status
) {
}