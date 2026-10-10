package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.EnvironmentCommandService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.EnvironmentQueryService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.LaboratoryQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentsByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetLaboratoryByIdQuery;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.AssignEnvironmentUsageResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.CreateEnvironmentResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.EnvironmentResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.EnvironmentUsageAssignmentResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.UpdateEnvironmentResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.AssignEnvironmentUsageCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.EnvironmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.RegisterEnvironmentCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.UpdateEnvironmentCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes the environments of a laboratory or pharmaceutical warehouse.
 *
 * <p>Tenant isolation for {@code laboratoryId} and {@code environmentId} is enforced by the
 * IAM tenant interceptor. Registering, updating and classifying environments is reserved to
 * quality managers and administrators; any member of the laboratory can consult them.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments", produces = APPLICATION_JSON_VALUE)
public class LaboratoryEnvironmentsController {

    private static final String QUALITY_ROLES = "hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final EnvironmentCommandService environmentCommandService;
    private final EnvironmentQueryService environmentQueryService;
    private final LaboratoryQueryService laboratoryQueryService;

    public LaboratoryEnvironmentsController(EnvironmentCommandService environmentCommandService,
                                            EnvironmentQueryService environmentQueryService,
                                            LaboratoryQueryService laboratoryQueryService) {
        this.environmentCommandService = environmentCommandService;
        this.environmentQueryService = environmentQueryService;
        this.laboratoryQueryService = laboratoryQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Register an environment",
            description = "Registers a new environment inside the laboratory. The code must be unique within the laboratory.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Environment registered",
                    content = @Content(schema = @Schema(implementation = EnvironmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid environment data",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Laboratory not found"),
            @ApiResponse(responseCode = "409", description = "An environment with the same code already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerEnvironment(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1") Long laboratoryId,
            @Valid @RequestBody CreateEnvironmentResource resource) {
        var command = RegisterEnvironmentCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(
                environmentCommandService.handle(command),
                EnvironmentResourceFromEntityAssembler::toResourceFromEntity,
                Environment::getId);
    }

    @GetMapping
    @Operation(summary = "List the environments of a laboratory",
            description = "Returns the registered environments ordered by code. An empty list means the laboratory has no environments yet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Environments of the laboratory",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnvironmentResource.class)))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account"),
            @ApiResponse(responseCode = "404", description = "Laboratory not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getEnvironments(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1") Long laboratoryId) {
        if (laboratoryQueryService.handle(new GetLaboratoryByIdQuery(laboratoryId)).isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.notFound("Laboratory", laboratoryId));
        }
        var resources = environmentQueryService.handle(new GetEnvironmentsByLaboratoryIdQuery(laboratoryId)).stream()
                .map(EnvironmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{environmentId}")
    @Operation(summary = "Get an environment of a laboratory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Environment found",
                    content = @Content(schema = @Schema(implementation = EnvironmentResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getEnvironmentById(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1") Long laboratoryId,
            @PathVariable @Parameter(description = "Environment numeric identifier", example = "3") Long environmentId) {
        return environmentQueryService.handle(new GetEnvironmentByIdQuery(laboratoryId, environmentId))
                .<ResponseEntity<?>>map(environment ->
                        ResponseEntity.ok(EnvironmentResourceFromEntityAssembler.toResourceFromEntity(environment)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Environment", environmentId)));
    }

    @PutMapping(value = "/{environmentId}", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Update an environment",
            description = "Replaces the identification data (code, name, description) of the environment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Environment updated",
                    content = @Content(schema = @Schema(implementation = EnvironmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid environment data",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Another environment already uses the code",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> updateEnvironment(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1") Long laboratoryId,
            @PathVariable @Parameter(description = "Environment numeric identifier", example = "3") Long environmentId,
            @Valid @RequestBody UpdateEnvironmentResource resource) {
        var command = UpdateEnvironmentCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                environmentCommandService.handle(command),
                EnvironmentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @PostMapping(value = "/{environmentId}/usage-assignments", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Assign the usage of an environment",
            description = "Registers the main use of the environment: laboratory, production, raw material storage, product storage or other.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usage assigned",
                    content = @Content(schema = @Schema(implementation = EnvironmentUsageAssignmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Usage not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The environment already has the requested usage",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> assignUsage(
            @PathVariable @Parameter(description = "Laboratory numeric identifier", example = "1") Long laboratoryId,
            @PathVariable @Parameter(description = "Environment numeric identifier", example = "3") Long environmentId,
            @Valid @RequestBody AssignEnvironmentUsageResource resource) {
        var command = AssignEnvironmentUsageCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                environmentCommandService.handle(command),
                EnvironmentResourceFromEntityAssembler::toUsageAssignmentResourceFromEntity,
                HttpStatus.CREATED);
    }
}
