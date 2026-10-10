package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import java.util.List;

/**
 * Resource returned after successful authentication.
 *
 * @param id authenticated user identifier
 * @param username account username
 * @param token JWT token
 * @param roles assigned roles
 * @param laboratoryId associated laboratory identifier
 */
public record AuthenticatedUserResource(
        Long id,
        String username,
        String token,
        List<String> roles,
        Long laboratoryId,
        boolean passwordChangeRequired
) {
}