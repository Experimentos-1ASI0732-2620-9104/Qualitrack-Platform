package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.LaboratoryCommandService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.LaboratoryQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Laboratory;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetLaboratoryByIdQuery;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.CreateLaboratoryResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.LaboratoryResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.UpdateLaboratoryResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.CreateLaboratoryCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.LaboratoryResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.UpdateLaboratoryCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes laboratory resources and administration endpoints.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories", produces = APPLICATION_JSON_VALUE)
public class LaboratoryController {
    private final LaboratoryCommandService laboratoryCommandService;
    private final LaboratoryQueryService laboratoryQueryService;
    private final com.iotech.qualitrack.platform.laboratory.application.commandservices.LaboratoryOnboardingService onboardingService;

    public LaboratoryController(LaboratoryCommandService laboratoryCommandService, LaboratoryQueryService laboratoryQueryService,
            com.iotech.qualitrack.platform.laboratory.application.commandservices.LaboratoryOnboardingService onboardingService) {
        this.laboratoryCommandService = laboratoryCommandService;
        this.laboratoryQueryService = laboratoryQueryService;
        this.onboardingService = onboardingService;
    }

    @PostMapping
    @Operation(summary = "Create a new laboratory", description = "Registers a new pharmaceutical laboratory profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Laboratory created successfully", content = @Content(schema = @Schema(implementation = LaboratoryResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Conflict - Laboratory name already exists")
    })
    public ResponseEntity<?> createLaboratory(@RequestBody CreateLaboratoryResource resource,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl user) {
        var command = CreateLaboratoryCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = Result.<Long, ApplicationError>success(onboardingService.create(user.getId(), command))
                .flatMap(laboratoryId -> laboratoryQueryService.handle(new GetLaboratoryByIdQuery(laboratoryId))
                        .<Result<Laboratory, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("Laboratory", laboratoryId))));

        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(
                result,
                LaboratoryResourceFromEntityAssembler::toResourceFromEntity,
                laboratory -> org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/laboratories/{laboratoryId}").buildAndExpand(laboratory.getId()).toUri()
        );
    }

    @GetMapping("/{laboratoryId}")
    @Operation(summary = "Get laboratory by ID", description = "Retrieves a specific laboratory by its numeric identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Laboratory found", content = @Content(schema = @Schema(implementation = LaboratoryResource.class))),
            @ApiResponse(responseCode = "404", description = "Laboratory not found",
                    content = @Content(schema = @Schema(implementation = com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource.class)))
    })
    public ResponseEntity<?> getLaboratoryById(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1", required = true) Long laboratoryId
    ) {
        return laboratoryQueryService.handle(new GetLaboratoryByIdQuery(laboratoryId))
                .<ResponseEntity<?>>map(laboratory -> ResponseEntity.ok(LaboratoryResourceFromEntityAssembler.toResourceFromEntity(laboratory)))
                .orElseGet(() -> com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler
                        .toErrorResponseFromApplicationError(ApplicationError.notFound("Laboratory", laboratoryId)));
    }

    @PutMapping("/{laboratoryId}")
    @PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Update laboratory profile",
            description = "Updates an existing laboratory's basic information. Reserved to the quality managers of the laboratory.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Laboratory updated successfully", content = @Content(schema = @Schema(implementation = LaboratoryResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Laboratory not found")
    })
    public ResponseEntity<?> updateLaboratory(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1", required = true) Long laboratoryId,
            @RequestBody UpdateLaboratoryResource resource
    ) {
        var command = UpdateLaboratoryCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource);
        var result = laboratoryCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                LaboratoryResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }
}
