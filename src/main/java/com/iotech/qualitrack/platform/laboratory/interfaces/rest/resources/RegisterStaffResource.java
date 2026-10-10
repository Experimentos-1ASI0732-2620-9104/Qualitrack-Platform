package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register a staff member, who receives an account to sign in.
 */
@Schema(name = "RegisterStaffRequest", description = "Staff member to register; the e-mail becomes the username")
public record RegisterStaffResource(
        @Schema(description = "Full legal name", example = "Jane Doe")
        @NotBlank @Size(max = 150) String fullName,
        @Schema(description = "Job title in the laboratory", example = "Quality Inspector")
        @NotBlank @Size(max = 100) String role,
        @Schema(description = "Corporate email address; the credentials are sent to it", example = "jane.doe@pharmacorp.com")
        @NotBlank @Email @Size(max = 150) String email,
        @Schema(description = "OPERATOR registers the operations assigned to them; AUDITOR only consults", example = "OPERATOR",
                allowableValues = {"OPERATOR", "AUDITOR"})
        @NotBlank String accessRole
) {
}
