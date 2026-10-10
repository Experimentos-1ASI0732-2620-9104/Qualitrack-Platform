package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ChangePasswordCommand;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.ChangePasswordResource;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Password of the authenticated user.
 */
@RestController
@RequestMapping(value = "/api/v1/users/me/password-changes", produces = APPLICATION_JSON_VALUE)
public class UserPasswordController {
    private final UserCommandService userCommandService;

    public UserPasswordController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Change the password",
            description = "Replaces the password of the authenticated user. A staff member must do it with the temporary "
                    + "password received before using the platform.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Current password not correct or new password not valid",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetailsImpl user,
                                            @Valid @RequestBody ChangePasswordResource resource) {
        var result = userCommandService.handle(new ChangePasswordCommand(user.getId(), resource.currentPassword(),
                resource.newPassword()));
        return switch (result) {
            case Result.Success<User, ApplicationError> success -> ResponseEntity.noContent().build();
            case Result.Failure<User, ApplicationError> failure -> ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }
}
