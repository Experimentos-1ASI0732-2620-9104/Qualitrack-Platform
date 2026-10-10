package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.SignInResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.SignUpResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for IAM authentication operations.
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = APPLICATION_JSON_VALUE)
public class AuthenticationController {

    private final UserCommandService userCommandService;

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping("/sign-in")
    @Operation(summary = "User sign-in", description = "Authenticates a user and returns a JWT token.")
    public ResponseEntity<?> signIn(@RequestBody SignInResource resource) {
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                AuthenticatedUserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }

    @PostMapping("/sign-up")
    @Operation(summary = "User sign-up", description = "Registers a new IAM user account (TS03). The Location header points to the new user.")
    public ResponseEntity<?> signUp(@RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);

        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(
                result,
                UserResourceFromEntityAssembler::toResourceFromEntity,
                user -> ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/users/{userId}").buildAndExpand(user.getId()).toUri()
        );
    }
}