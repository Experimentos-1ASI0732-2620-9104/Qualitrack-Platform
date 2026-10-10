package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Staff member of a laboratory.
 */
@Schema(name = "StaffMemberResponse", description = "Staff member of the laboratory")
public record StaffMemberResource(
        @Schema(description = "Staff member unique numeric identifier", example = "1") Long id,
        @Schema(description = "Associated laboratory numeric identifier", example = "1") Long laboratoryId,
        @Schema(description = "Full legal name", example = "Jane Doe") String fullName,
        @Schema(description = "Job title in the laboratory", example = "Quality Inspector") String role,
        @Schema(description = "Corporate email address, also the username", example = "jane.doe@pharmacorp.com") String email,
        @Schema(description = "Is the staff member currently active", example = "true") boolean active,
        @Schema(description = "What the staff member can do; null for staff registered before accounts existed",
                example = "OPERATOR", allowableValues = {"OPERATOR", "AUDITOR"}, nullable = true) String accessRole,
        @Schema(description = "Account with which the staff member signs in; null for staff registered before accounts existed",
                example = "12", nullable = true) Long userId
) {
}
