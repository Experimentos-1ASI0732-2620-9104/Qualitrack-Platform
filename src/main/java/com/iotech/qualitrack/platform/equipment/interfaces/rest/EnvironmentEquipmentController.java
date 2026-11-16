package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.AssignEquipmentToEnvironmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByEnvironmentIdQuery;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.AssignEquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.ChangeEquipmentStatusResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentStatusChangeResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.ChangeEquipmentStatusCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentLocationUriAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentStatusChangeResourceFromEntityAssembler;
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

import java.util.Comparator;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Equipment located in an environment: listing, association and operational status changes (US47, US48, TS33,
 * TS34). The listing also gives the Edge of the environment its IoT devices.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/equipments", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentEquipmentController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final EquipmentCommandService equipmentCommandService;
    private final EquipmentQueryService equipmentQueryService;

    /**
     * Creates the controller with the equipment command and query services.
     *
     * @param equipmentCommandService locates equipment and changes its status
     * @param equipmentQueryService lists the equipment of the environment
     */
    public EnvironmentEquipmentController(EquipmentCommandService equipmentCommandService,
                                          EquipmentQueryService equipmentQueryService) {
        this.equipmentCommandService = equipmentCommandService;
        this.equipmentQueryService = equipmentQueryService;
    }

    /**
     * Lists the equipment located in the environment, by name.
     *
     * <p>With {@code iot=true} it returns only the IoT devices, which is what the Edge of the environment reads
     * to know its nodes instead of the devices of the whole laboratory.</p>
     *
     * @param laboratoryId laboratory that owns the environment
     * @param environmentId environment whose equipment is listed
     * @param iot whether to return only the IoT devices
     * @return 200 with the equipment; an environment outside the laboratory is refused with 403 by the tenant
     *         access check, and with 404 if it ever reaches the query
     */
    @GetMapping
    @Operation(summary = "List equipment of the environment",
            description = "Equipment and IoT devices located in the environment, by name. With iot=true it returns only the "
                    + "IoT devices, which the Edge of the environment uses to know its nodes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipment located in the environment",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipmentResource.class)))),
            @ApiResponse(responseCode = "403", description = "Laboratory or environment not available to the account, "
                    + "including an environment of another laboratory or one that does not exist")
    })
    public ResponseEntity<?> getEnvironmentEquipment(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                     @Parameter(description = "Return only the IoT devices")
                                                     @RequestParam(defaultValue = "false") boolean iot) {
        return equipmentQueryService.handle(new GetEquipmentByEnvironmentIdQuery(laboratoryId, environmentId, iot))
                .<ResponseEntity<?>>map(equipment -> ResponseEntity.ok(equipment.stream()
                        .sorted(Comparator.comparing(Equipment::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
                        .toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Environment", environmentId)));
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
