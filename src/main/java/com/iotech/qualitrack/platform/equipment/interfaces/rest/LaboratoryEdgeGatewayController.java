package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterEdgeGatewayResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentLocationUriAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.RegisterEdgeGatewayCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller responsible for Edge Gateway registration.
 *
 * <p>Provides a dedicated registration endpoint without
 * modifying ordinary equipment or IoT device controllers.</p>
 *
 * <p>Only authorized laboratory members with administrative
 * or quality management permissions can register Gateways.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/edge-gateways", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) " + "and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')")
public class LaboratoryEdgeGatewayController {

    private final EquipmentCommandService equipmentCommandService;

    public LaboratoryEdgeGatewayController(EquipmentCommandService equipmentCommandService) {
        this.equipmentCommandService = equipmentCommandService;
    }

    /**
     * Registers an Edge Gateway with a unique external identifier.
     *
     * @param laboratoryId laboratory that owns the Gateway
     * @param resource Gateway registration request
     * @return registered Gateway or application error
     */
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Register an Edge Gateway",
            description = "Registers an Edge Gateway as laboratory equipment "
                    + "using a unique external identifier. "
                    + "Authentication credentials are provisioned separately."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Edge Gateway registered successfully", content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Gateway registration data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient laboratory permissions"),
            @ApiResponse(responseCode = "404", description = "Laboratory not found", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Gateway external identifier or serial number already exists", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> register(
            @PathVariable Long laboratoryId,
            @Valid @RequestBody RegisterEdgeGatewayResource resource) {

        var command = RegisterEdgeGatewayCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource);

        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(
                equipmentCommandService.handle(command),
                EquipmentResourceFromEntityAssembler::toResourceFromEntity,
                EquipmentLocationUriAssembler::toUriFromEntity
        );
    }
}