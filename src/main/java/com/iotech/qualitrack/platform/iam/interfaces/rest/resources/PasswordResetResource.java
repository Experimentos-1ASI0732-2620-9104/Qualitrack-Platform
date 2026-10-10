package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body to set a new password with the verification code (TS06).
 */
@Schema(name = "PasswordResetRequest", description = "Verification code received by e-mail and the new password")
public record PasswordResetResource(
        @Schema(description = "Username or e-mail used to request the code") @NotBlank String account,
        @Schema(description = "Verification code of 6 digits", example = "482913") @NotBlank @Pattern(regexp = "\\d{6}") String code,
        @Schema(description = "New password: 8 to 72 characters with letters and digits")
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
