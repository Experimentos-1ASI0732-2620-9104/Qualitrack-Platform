package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.AssignEquipmentToEnvironmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.AssignDeviceResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentLocationUriAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
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
 * Association of IoT devices with the environment they supervise (US52, US54, TS38, TS40).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')")
public class EnvironmentDevicesController {
    private final EquipmentCommandService equipmentCommandService;

    public EnvironmentDevicesController(EquipmentCommandService equipmentCommandService) {
        this.equipmentCommandService = equipmentCommandService;
    }

    @PostMapping(value = "/environmental-devices", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Associate an environmental device with the environment",
            description = "The environmental device supervises the environment; an environment has at most one.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Environmental device located in the environment",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Environmental device or environment not registered in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The environment already has another environmental device",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> associateEnvironmentalDevice(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                          @Valid @RequestBody AssignDeviceResource resource) {
        return associate(laboratoryId, environmentId, resource, IotDeviceType.ENVIRONMENTAL_DEVICE);
    }

    @PostMapping(value = "/container-monitors", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Associate a container monitor with the environment",
            description = "Relates the measurements of the container monitor with the environment where the container is kept.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Container monitor located in the environment",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Container monitor or environment not registered in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> associateContainerMonitor(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                       @Valid @RequestBody AssignDeviceResource resource) {
        return associate(laboratoryId, environmentId, resource, IotDeviceType.CONTAINER_MONITOR);
    }

    private ResponseEntity<?> associate(Long laboratoryId, Long environmentId, AssignDeviceResource resource, IotDeviceType deviceType) {
        var command = new AssignEquipmentToEnvironmentCommand(laboratoryId, environmentId, resource.deviceId(), deviceType);
        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(equipmentCommandService.handle(command),
                EquipmentResourceFromEntityAssembler::toResourceFromEntity, EquipmentLocationUriAssembler::toUriFromEntity);
    }
}
