package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterIotDeviceResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentLocationUriAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.RegisterIotDeviceCommandFromResourceAssembler;
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
 * Registration of the ESP32 IoT devices of a laboratory (US51, US53, TS37, TS39).
 * A registered device is an equipment; its Location points to {@code /laboratories/{laboratoryId}/equipments/{id}}.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/devices", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')")
public class LaboratoryDevicesController {
    private final EquipmentCommandService equipmentCommandService;

    public LaboratoryDevicesController(EquipmentCommandService equipmentCommandService) {
        this.equipmentCommandService = equipmentCommandService;
    }

    @PostMapping(value = "/environmental-devices", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register an environmental device",
            description = "Registers the ESP32 node that supervises an environment, with the identity Edge uses to recognise it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Environmental device registered",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "409", description = "Device identifier or serial number already registered", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerEnvironmentalDevice(@PathVariable Long laboratoryId, @Valid @RequestBody RegisterIotDeviceResource resource) {
        return register(laboratoryId, IotDeviceType.ENVIRONMENTAL_DEVICE, resource);
    }

    @PostMapping(value = "/container-monitors", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a container monitor",
            description = "Registers the ESP32 monitor of a container, with the identity Edge uses to recognise it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Container monitor registered",
                    content = @Content(schema = @Schema(implementation = EquipmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "409", description = "Device identifier or serial number already registered", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerContainerMonitor(@PathVariable Long laboratoryId, @Valid @RequestBody RegisterIotDeviceResource resource) {
        return register(laboratoryId, IotDeviceType.CONTAINER_MONITOR, resource);
    }

    private ResponseEntity<?> register(Long laboratoryId, IotDeviceType deviceType, RegisterIotDeviceResource resource) {
        var command = RegisterIotDeviceCommandFromResourceAssembler.toCommandFromResource(laboratoryId, deviceType, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(equipmentCommandService.handle(command),
                EquipmentResourceFromEntityAssembler::toResourceFromEntity, EquipmentLocationUriAssembler::toUriFromEntity);
    }
}
