package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterEquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.RegisterEquipmentCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Equipment and IoT devices registered in a laboratory (US45, US46, TS31, TS32).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/equipments", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class LaboratoryEquipmentController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final EquipmentCommandService equipmentCommandService;
    private final EquipmentQueryService equipmentQueryService;

    public LaboratoryEquipmentController(EquipmentCommandService equipmentCommandService,
                                         EquipmentQueryService equipmentQueryService) {
        this.equipmentCommandService = equipmentCommandService;
        this.equipmentQueryService = equipmentQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Register an equipment", description = "Registers an equipment of the laboratory with a unique serial number.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipment registered",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "409", description = "Serial number already registered", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerEquipment(@PathVariable Long laboratoryId, @Valid @RequestBody RegisterEquipmentResource resource) {
        var command = RegisterEquipmentCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(equipmentCommandService.handle(command),
                EquipmentResourceFromEntityAssembler::toResourceFromEntity, Equipment::getId);
    }

    @GetMapping
    @Operation(summary = "List equipment", description = "Equipment and IoT devices of the laboratory with their status, by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipment of the laboratory",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account")
    })
    public ResponseEntity<List<EquipmentResource>> getEquipment(@PathVariable Long laboratoryId) {
        return ResponseEntity.ok(equipmentQueryService.handle(new GetEquipmentByLabIdQuery(laboratoryId)).stream()
                .sorted(Comparator.comparing(Equipment::getName, String.CASE_INSENSITIVE_ORDER))
                .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/{equipmentId}")
    @Operation(summary = "Get an equipment", description = "One equipment or IoT device of the laboratory.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipment found",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "403", description = "Equipment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Equipment not registered in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getEquipmentById(@PathVariable Long laboratoryId, @PathVariable Long equipmentId) {
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(laboratoryId, equipmentId))
                .<ResponseEntity<?>>map(equipment -> ResponseEntity.ok(EquipmentResourceFromEntityAssembler.toResourceFromEntity(equipment)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Equipment", equipmentId)));
    }
}
