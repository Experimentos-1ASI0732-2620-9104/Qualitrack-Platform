package com.iotech.qualitrack.platform.profile.interfaces.rest;

import com.iotech.qualitrack.platform.profile.application.queryservices.ProfileQueryService;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfileQuery;
import com.iotech.qualitrack.platform.profile.interfaces.rest.resources.ProfileResource;
import com.iotech.qualitrack.platform.profile.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Profile of a staff member as the quality manager of the laboratory sees it. The DNI, the location and the photo are
 * personal data, so other staff members and auditors cannot read them.
 */
@RestController
@RequestMapping("/api/v1/laboratories/{laboratoryId}/staff/{staffId}/profile")
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')")
public class StaffProfileController {

    private final ProfileQueryService profileQueryService;

    public StaffProfileController(ProfileQueryService profileQueryService) {
        this.profileQueryService = profileQueryService;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the profile of a staff member",
            description = "Personal data the staff member keeps in the profile, with the account they sign in with. "
                    + "Only for the quality manager of the laboratory.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile of the staff member",
                    content = @Content(schema = @Schema(implementation = ProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Not the quality manager of the laboratory"),
            @ApiResponse(responseCode = "404", description = "Staff member not found or without account",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getStaffProfile(@PathVariable Long laboratoryId, @PathVariable Long staffId) {
        return profileQueryService.handle(new GetStaffProfileQuery(laboratoryId, staffId))
                .<ResponseEntity<?>>map(detail -> ResponseEntity.ok(ProfileResourceFromEntityAssembler.toResourceFromDetail(detail)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("StaffProfile", staffId)));
    }

    @GetMapping("/photo")
    @Operation(summary = "Get the photo of a staff member", description = "Only for the quality manager of the laboratory.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo (JPEG, PNG or WebP)", content = {
                    @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "403", description = "Not the quality manager of the laboratory"),
            @ApiResponse(responseCode = "404", description = "Staff member without account or without photo",
                    content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getStaffPhoto(@PathVariable Long laboratoryId, @PathVariable Long staffId) {
        return ProfilePhotoResponses.toPhotoResponse(staffId,
                profileQueryService.handle(new GetStaffProfilePhotoQuery(laboratoryId, staffId)));
    }
}
