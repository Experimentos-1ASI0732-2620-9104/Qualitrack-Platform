package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Credentials of the account created for a staff member.
 */
@Schema(name = "StaffCredentialsResponse", description = "How the staff member receives the credentials of their account")
public record StaffCredentialsResource(
        @Schema(description = "Username of the account (the staff member e-mail)", example = "jane.doe@pharmacorp.com") String username,
        @Schema(description = "EMAIL when the credentials were e-mailed; SHOWN_ONCE when the quality manager must hand them over",
                example = "EMAIL", allowableValues = {"EMAIL", "SHOWN_ONCE"}) String delivery,
        @Schema(description = "Temporary password, only returned when it could not be e-mailed; it is not shown again",
                nullable = true) String temporaryPassword
) {
}
