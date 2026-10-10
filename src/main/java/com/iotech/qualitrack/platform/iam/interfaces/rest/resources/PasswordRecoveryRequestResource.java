package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body to start the recovery of a password (TS05).
 */
@Schema(name = "PasswordRecoveryRequest", description = "Account whose password is recovered")
public record PasswordRecoveryRequestResource(
        @Schema(description = "Username or e-mail of the account", example = "qa.manager@laboratorio.pe")
        @NotBlank String account
) {
}
