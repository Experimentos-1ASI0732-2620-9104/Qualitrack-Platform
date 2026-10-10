package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateAccountRequest", description = "Username and e-mail of the authenticated user, confirmed with the current password",
        example = "{\"username\": \"maria.quality\", \"email\": \"maria@labsur.pe\", \"currentPassword\": \"********\"}")
public record UpdateAccountResource(
        @Schema(description = "Username used to sign in (3 to 80 characters)")
        @NotBlank @Size(min = 3, max = 80) String username,
        @Schema(description = "E-mail used for password recovery and notifications")
        @NotBlank @Email @Size(max = 120) String email,
        @Schema(description = "Password used to sign in")
        @NotBlank String currentPassword
) {
}
