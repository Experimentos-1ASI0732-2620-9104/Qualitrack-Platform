package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.AssignEquipmentToEnvironmentCommand;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.AssignEquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.ChangeEquipmentStatusResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentStatusChangeResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.ChangeEquipmentStatusCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentLocationUriAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentStatusChangeResourceFromEntityAssembler;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Equipment located in an environment: association and operational status changes (US47, US48, TS33, TS34).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/equipments", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentEquipmentController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final EquipmentCommandService equipmentCommandService;

    public EnvironmentEquipmentController(EquipmentCommandService equipmentCommandService) {
        this.equipmentCommandService = equipmentCommandService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Associate an equipment with the environment",
            description = "Records the environment where an equipment of the laboratory is located; moving it replaces the previous location.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipment located in the environment",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Equipment or environment not registered in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The environment already has an environmental device", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> associateEquipment(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                @Valid @RequestBody AssignEquipmentResource resource) {
        var command = new AssignEquipmentToEnvironmentCommand(laboratoryId, environmentId, resource.equipmentId(), null);
        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(equipmentCommandService.handle(command),
                EquipmentResourceFromEntityAssembler::toResourceFromEntity, EquipmentLocationUriAssembler::toUriFromEntity);
    }

    @PostMapping(value = "/{equipmentId}/status-changes", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a status change",
            description = "Changes the operational status of an equipment located in the environment and keeps the change for traceability.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Status change registered",
                    content = @Content(schema = @Schema(implementation = EquipmentStatusChangeResource.class))),
            @ApiResponse(responseCode = "400", description = "Status not allowed or equal to the current one; the current status is kept",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Equipment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Equipment not located in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerStatusChange(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                  @PathVariable Long equipmentId,
                                                  @Valid @RequestBody ChangeEquipmentStatusResource resource) {
        var command = ChangeEquipmentStatusCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, equipmentId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(equipmentCommandService.handle(command),
                EquipmentStatusChangeResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }
}
