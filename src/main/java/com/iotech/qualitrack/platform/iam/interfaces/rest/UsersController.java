package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.application.queryservices.UserQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.AssignRoleCommand;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.UserResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * IAM user accounts of the laboratory of the authenticated user. Staff accounts are deactivated through
 * {@code POST /laboratories/{laboratoryId}/staff/{staffId}/deactivations}.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
public class UsersController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    public UsersController(UserQueryService userQueryService, UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    @GetMapping
    @Operation(summary = "Get users", description = "Users of the laboratory of the authenticated user.")
    public ResponseEntity<List<UserResource>> getUsers(@AuthenticationPrincipal UserDetailsImpl principal) {
        var resources = userQueryService.handle(new GetAllUsersQuery()).stream()
                .filter(user -> principal.getLaboratoryId() != null && principal.getLaboratoryId().equals(user.getLaboratoryId()))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "403", description = "User not available to the account"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("User", userId)));
    }

    @PutMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a role to a user", description = "Adds the role to the user; assigning it again has no effect.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User with the role",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown role",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @PathVariable String roleName) {
        var result = userCommandService.handle(new AssignRoleCommand(userId, roleName))
                .flatMap(id -> userQueryService.handle(new GetUserByIdQuery(id))
                        .<Result<User, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("User", id))));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                UserResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }
}
