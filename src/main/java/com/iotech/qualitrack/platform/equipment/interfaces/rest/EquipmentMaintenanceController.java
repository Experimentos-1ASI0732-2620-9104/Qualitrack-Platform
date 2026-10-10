package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.MaintenanceCommandService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.MaintenanceQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetMaintenanceByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.MaintenanceRecordResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterMaintenanceResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.MaintenanceResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.RegisterMaintenanceCommandFromResourceAssembler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Maintenance history of an equipment located in an environment (US49, US50, TS35, TS36).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/equipments/{equipmentId}/maintenance-records",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EquipmentMaintenanceController {

    private final MaintenanceCommandService maintenanceCommandService;
    private final MaintenanceQueryService maintenanceQueryService;

    public EquipmentMaintenanceController(MaintenanceCommandService maintenanceCommandService,
                                          MaintenanceQueryService maintenanceQueryService) {
        this.maintenanceCommandService = maintenanceCommandService;
        this.maintenanceQueryService = maintenanceQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a maintenance", description = "Keeps the evidence of an intervention performed on the equipment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Maintenance registered",
                    content = @Content(schema = @Schema(implementation = MaintenanceRecordResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data or date in the future", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Equipment not available to the account, or an operator choosing another technician"),
            @ApiResponse(responseCode = "404", description = "Equipment not located in the environment or technician not in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerMaintenance(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                 @PathVariable Long equipmentId,
                                                 @Valid @RequestBody RegisterMaintenanceResource resource) {
        var command = RegisterMaintenanceCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, equipmentId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(maintenanceCommandService.handle(command),
                MaintenanceResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List maintenance records", description = "Maintenance performed on the equipment, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance history of the equipment",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceRecordResource.class)))),
            @ApiResponse(responseCode = "403", description = "Equipment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Equipment not located in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getMaintenanceHistory(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                   @PathVariable Long equipmentId) {
        return maintenanceQueryService.handle(new GetMaintenanceByEquipmentIdQuery(laboratoryId, environmentId, equipmentId))
                .<ResponseEntity<?>>map(records -> ResponseEntity.ok(records.stream()
                        .map(MaintenanceResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Equipment", equipmentId)));
    }
}
