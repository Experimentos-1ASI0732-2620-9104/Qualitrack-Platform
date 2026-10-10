package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Answer to a password recovery request; it is the same whether or not the account exists.
 */
@Schema(name = "PasswordRecoveryAccepted", description = "The verification code is sent to the e-mail of the account, "
        + "when it exists and has one")
public record PasswordRecoveryAcceptedResource(
        @Schema(description = "Minutes during which the code can be used", example = "15") long codeValidityMinutes
) {
}
