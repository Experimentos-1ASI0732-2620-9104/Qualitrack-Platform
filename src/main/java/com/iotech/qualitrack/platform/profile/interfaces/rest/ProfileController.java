package com.iotech.qualitrack.platform.profile.interfaces.rest;

import com.iotech.qualitrack.platform.profile.application.commandservices.ProfileCommandService;
import com.iotech.qualitrack.platform.profile.application.queryservices.ProfileQueryService;
import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.commands.ChangeProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.RemoveProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.UpdateProfileCommand;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfileByUserIdQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfileDetail;
import com.iotech.qualitrack.platform.profile.interfaces.rest.resources.ProfileResource;
import com.iotech.qualitrack.platform.profile.interfaces.rest.resources.UpdateProfileResource;
import com.iotech.qualitrack.platform.profile.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Profile of the authenticated user: personal data and photo.
 */
@RestController
@RequestMapping("/api/v1/users/me/profile")
public class ProfileController {
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String WEBP = "image/webp";

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;
    private final CurrentUser currentUser;

    public ProfileController(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService,
                             CurrentUser currentUser) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
        this.currentUser = currentUser;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the profile of the authenticated user",
            description = "Personal data with the username and e-mail of the account. A profile never saved comes "
                    + "with the name the quality manager registered for a staff member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile",
                    content = @Content(schema = @Schema(implementation = ProfileResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> getProfile() {
        return profileQueryService.handle(new GetProfileByUserIdQuery(currentUser.userId()))
                .<ResponseEntity<?>>map(detail -> ResponseEntity.ok(ProfileResourceFromEntityAssembler.toResourceFromDetail(detail)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("User", currentUser.userId())));
    }

    @PutMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the profile of the authenticated user",
            description = "Replaces the full name, DNI, phone number and location; empty optional fields are cleared. "
                    + "The staff list of the laboratory shows the new name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = ProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing full name, or DNI, phone number or location not valid",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileResource resource) {
        var command = UpdateProfileCommand.of(currentUser.userId(), resource.fullName(), resource.dni(),
                resource.phoneNumber(), resource.location());
        return toProfileResponse(profileCommandService.handle(command));
    }

    @GetMapping("/photo")
    @Operation(summary = "Get the photo of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo (JPEG, PNG or WebP)", content = {
                    @Content(mediaType = JPEG, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = PNG, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = WEBP, schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "404", description = "The profile has no photo",
                    content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getPhoto() {
        return ProfilePhotoResponses.toPhotoResponse(currentUser.userId(),
                profileQueryService.handle(new GetProfilePhotoQuery(currentUser.userId())));
    }

    @PutMapping(value = "/photo", consumes = {JPEG, PNG, WEBP}, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace the photo of the authenticated user",
            description = "The body is the image itself (JPEG, PNG or WebP, at most 2 MB). The type is checked "
                    + "against the content of the file.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = {
            @Content(mediaType = JPEG, schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = PNG, schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = WEBP, schema = @Schema(type = "string", format = "binary"))})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo replaced",
                    content = @Content(schema = @Schema(implementation = ProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "Empty file or not a JPEG, PNG or WebP image",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "413", description = "The image exceeds 2 MB",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "415", description = "The body is not a JPEG, PNG or WebP image",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> replacePhoto(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > PhotoImage.MAX_SIZE_BYTES) return ProfilePhotoResponses.tooLarge();
        var content = request.getInputStream().readNBytes(PhotoImage.MAX_SIZE_BYTES + 1);
        if (content.length > PhotoImage.MAX_SIZE_BYTES) return ProfilePhotoResponses.tooLarge();
        var command = new ChangeProfilePhotoCommand(currentUser.userId(), PhotoImage.of(content));
        return toProfileResponse(profileCommandService.handle(command));
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Remove the photo of the authenticated user",
            description = "Removing a photo that does not exist changes nothing.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The profile has no photo"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> removePhoto() {
        return switch (profileCommandService.handle(new RemoveProfilePhotoCommand(currentUser.userId()))) {
            case Result.Success<Profile, ApplicationError> success -> ResponseEntity.noContent().build();
            case Result.Failure<Profile, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    /**
     * Answers with the current profile after a successful change.
     */
    private ResponseEntity<?> toProfileResponse(Result<Profile, ApplicationError> change) {
        var userId = currentUser.userId();
        Result<ProfileDetail, ApplicationError> result = change.flatMap(profile -> profileQueryService
                .handle(new GetProfileByUserIdQuery(userId))
                .<Result<ProfileDetail, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound("User", userId))));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                ProfileResourceFromEntityAssembler::toResourceFromDetail, HttpStatus.OK);
    }
}
