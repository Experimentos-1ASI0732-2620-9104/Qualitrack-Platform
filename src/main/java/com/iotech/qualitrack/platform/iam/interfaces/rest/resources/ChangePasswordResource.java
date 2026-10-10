package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to replace the password of the authenticated user.
 */
@Schema(name = "ChangePasswordRequest", description = "Current password and the new password of the authenticated user")
public record ChangePasswordResource(
        @Schema(description = "Password used to sign in (the temporary password of a new staff account)")
        @NotBlank String currentPassword,
        @Schema(description = "New password: 8 to 72 characters with letters and digits")
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
