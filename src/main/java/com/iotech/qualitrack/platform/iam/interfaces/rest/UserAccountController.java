package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.application.queryservices.UserQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.commands.UpdateAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.UpdateAccountResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.UserResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Account of the authenticated user: the username used to sign in and the e-mail.
 */
@RestController
@RequestMapping(value = "/api/v1/users/me", produces = APPLICATION_JSON_VALUE)
public class UserAccountController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    public UserAccountController(UserQueryService userQueryService, UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    @GetMapping
    @Operation(summary = "Get the account of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> getAccount(@AuthenticationPrincipal UserDetailsImpl user) {
        return userQueryService.handle(new GetUserByIdQuery(user.getId()))
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(account)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("User", user.getId())));
    }

    @PutMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the username and the e-mail",
            description = "Replaces the username and the e-mail of the authenticated user after checking the current "
                    + "password. Neither may be used by another account, as username or as e-mail. The session token "
                    + "identifies the user by username, so a new token is returned and the previous one stops working.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated with a new session token",
                    content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid username or e-mail, or current password not correct",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Username or e-mail used by another account",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserDetailsImpl user,
                                           @Valid @RequestBody UpdateAccountResource resource) {
        var command = UpdateAccountCommand.of(user.getId(), resource.username(), resource.email(), resource.currentPassword());
        return ResponseEntityAssembler.toResponseEntityFromResult(userCommandService.handle(command),
                AuthenticatedUserResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }
}
