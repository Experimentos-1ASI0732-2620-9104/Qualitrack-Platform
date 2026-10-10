package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Answer to a successful password reset.
 */
@Schema(name = "PasswordResetCompleted", description = "The password was replaced; the account signs in with it")
public record PasswordResetCompletedResource(@Schema(description = "Username to sign in") String username) {
}
