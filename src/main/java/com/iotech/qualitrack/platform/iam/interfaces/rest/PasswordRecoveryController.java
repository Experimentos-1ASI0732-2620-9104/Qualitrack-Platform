package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.commandservices.PasswordRecoveryCommandService;
import com.iotech.qualitrack.platform.iam.domain.model.commands.RequestPasswordRecoveryCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.PasswordRecoveryAcceptedResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.PasswordRecoveryRequestResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.PasswordResetCompletedResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.PasswordResetResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Recovery of the password of an account with a verification code sent to its e-mail (TS05, TS06). Both operations
 * are public: the person cannot sign in.
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = APPLICATION_JSON_VALUE)
public class PasswordRecoveryController {
    private final PasswordRecoveryCommandService recoveries;

    public PasswordRecoveryController(PasswordRecoveryCommandService recoveries) {
        this.recoveries = recoveries;
    }

    @PostMapping(value = "/password-recovery-requests", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Request a password recovery",
            description = "Sends a verification code of 6 digits to the e-mail of the account (US16, TS05). The code "
                    + "expires in 15 minutes, can be used once and a new one is sent at most once a minute. The answer "
                    + "is the same whether or not the account exists, so it does not reveal registered accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Request accepted",
                    content = @Content(schema = @Schema(implementation = PasswordRecoveryAcceptedResource.class))),
            @ApiResponse(responseCode = "400", description = "The username or e-mail is missing",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> requestRecovery(@Valid @RequestBody PasswordRecoveryRequestResource resource) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                recoveries.handle(new RequestPasswordRecoveryCommand(resource.account())),
                validity -> new PasswordRecoveryAcceptedResource(validity.toMinutes()), HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/password-resets", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Reset the password with the verification code",
            description = "Sets the new password when the code matches the pending recovery of the account (US17, "
                    + "TS06). A wrong, used, replaced or expired code is rejected; five wrong codes revoke the recovery.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password replaced",
                    content = @Content(schema = @Schema(implementation = PasswordResetCompletedResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid code or password",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetResource resource) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                recoveries.handle(new ResetPasswordCommand(resource.account(), resource.code(), resource.newPassword())),
                user -> new PasswordResetCompletedResource(user.getUsernameValue()), HttpStatus.OK);
    }
}
