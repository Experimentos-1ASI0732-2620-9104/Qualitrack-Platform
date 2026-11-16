package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.tracking.application.commandservices.TrackingCommandService;
import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.MeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordMeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.MeasurementResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.RecordMeasurementCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.TelemetryResponseAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.ActuationEventResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordActuationEventResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.ActuationEventResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.RecordActuationEventCommandFromResourceAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for the readings and actions sent by authenticated Edge Gateways (TS54, TS55, TS56).
 *
 * <p>The Gateway authenticates with its API key ({@code X-API-Key}) and gets the {@code ROLE_EDGE_GATEWAY}
 * authority. The laboratory and environment come from the authenticated Gateway, never from the request, and are
 * used to check that the IoT device is located in that environment and served by that Gateway; otherwise the
 * request is rejected with 403.</p>
 *
 * <p>The records are processed through the same Tracking command service used by the laboratory endpoints,
 * preserving validation, idempotency and environmental threshold evaluation.</p>
 */
@RestController
@RequestMapping(
        value = "/api/v1/edge-gateways",
        produces = APPLICATION_JSON_VALUE
)
@PreAuthorize("hasAuthority('ROLE_EDGE_GATEWAY')")
public class EdgeGatewayTelemetryController {

    private final TrackingCommandService trackingCommandService;
    private final TrackingExternalEquipmentService equipmentService;

    public EdgeGatewayTelemetryController(
            TrackingCommandService trackingCommandService,
            TrackingExternalEquipmentService equipmentService) {

        this.trackingCommandService = trackingCommandService;
        this.equipmentService = equipmentService;
    }

    /**
     * Records a measurement sent by an authenticated Gateway.
     *
     * <p>The Gateway can only report measurements for the IoT devices
     * it serves in its current laboratory and environment.</p>
     *
     * <p>The external Gateway ID must match the authenticated
     * principal. Environmental readings use the environment's
     * environmental device, while container readings use the
     * identified container monitor.</p>
     *
     * @param gatewayId Gateway external identifier from the URL
     * @param iotDeviceId numeric Equipment identifier of the ESP32
     * @param authenticatedGatewayId authenticated Gateway identifier
     * @param gatewayEquipmentId Equipment identifier of the authenticated Gateway
     * @param laboratoryId laboratory from Gateway authentication
     * @param environmentId environment from Gateway authentication
     * @param resource measurement received from Edge Service
     * @return 201 for a new reading, 200 for a duplicate,
     *         or an error response
     */
    @PostMapping(
            value = "/{gatewayId}/iot-devices/{iotDeviceId}/telemetry-measurements",
            consumes = APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Record IoT telemetry from an Edge Gateway",
            description = "Records a measurement for an IoT device "
                    + "assigned to the authenticated Gateway's environment. "
                    + "Repeated measurements with the same metric and "
                    + "timestamp are handled idempotently.",
            parameters = {
                    @Parameter(
                            name = "X-API-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "API key issued to the Edge Gateway",
                            schema = @Schema(
                                    type = "string",
                                    format = "password"
                            )
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Measurement recorded",
                    content = @Content(
                            schema = @Schema(
                                    implementation = MeasurementResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Measurement already received",
                    content = @Content(
                            schema = @Schema(
                                    implementation = MeasurementResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid Gateway API key"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Gateway cannot report for this IoT device"
            )
    })
    public ResponseEntity<?> recordMeasurement(
            @PathVariable String gatewayId,
            @PathVariable Long iotDeviceId,

            @AuthenticationPrincipal(expression = "deviceId()")
            String authenticatedGatewayId,

            @AuthenticationPrincipal(expression = "equipmentId()")
            Long gatewayEquipmentId,

            @AuthenticationPrincipal(expression = "laboratoryId()")
            Long laboratoryId,

            @AuthenticationPrincipal(expression = "environmentId()")
            Long environmentId,

            @Valid @RequestBody RecordMeasurementResource resource) {

        // Verify that the request targets the authenticated Gateway.
        if (!gatewayId.equals(authenticatedGatewayId)
                || laboratoryId == null
                || environmentId == null) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Verify that the IoT device is one of the devices this Gateway serves in its environment.
        var device = equipmentService.findDevice(
                laboratoryId,
                environmentId,
                iotDeviceId
        );

        if (device.isEmpty() || !equipmentService.isServedByEdgeGateway(
                laboratoryId, environmentId, gatewayEquipmentId, iotDeviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long commandDeviceId;

        if (device.get().isContainerMonitor()) {

            // Container monitors are identified by their Equipment ID.
            commandDeviceId = iotDeviceId;

        } else {

            // Environmental measurements belong to the single
            // environmental device assigned to this environment.
            boolean isEnvironmentalDevice = equipmentService
                    .findEnvironmentalDevice(laboratoryId, environmentId)
                    .filter(environmental ->
                            environmental.id().equals(iotDeviceId)
                    )
                    .isPresent();

            if (!isEnvironmentalDevice) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // The existing Tracking command uses null to identify
            // the environmental device of the environment.
            commandDeviceId = null;
        }

        var command = RecordMeasurementCommandFromResourceAssembler
                .toCommandFromResource(
                        laboratoryId,
                        environmentId,
                        commandDeviceId,
                        resource
                );

        return TelemetryResponseAssembler.toRecordedResponse(
                trackingCommandService.handle(command),
                MeasurementResourceFromEntityAssembler::toResourceFromEntity
        );
    }

    /**
     * Records an actuation event reported by an authenticated Edge Gateway.
     *
     * <p>Only the container monitors the Gateway serves in its current
     * laboratory and environment are allowed to report events.</p>
     *
     * <p>Delegates event persistence and idempotency to the existing
     * Tracking command service.</p>
     *
     * @param gatewayId external Gateway identifier
     * @param iotDeviceId container monitor Equipment identifier
     * @param authenticatedGatewayId authenticated Gateway identifier
     * @param gatewayEquipmentId Equipment identifier of the authenticated Gateway
     * @param laboratoryId laboratory obtained from Gateway authentication
     * @param environmentId environment obtained from Gateway authentication
     * @param resource actuation event request
     * @return recorded event or corresponding error response
     */
    @PostMapping(
            value = "/{gatewayId}/iot-devices/{iotDeviceId}/actuation-events",
            consumes = APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Record Edge Gateway actuation event",
            description = "Records an actuation event executed by a "
                    + "container monitor assigned to the authenticated "
                    + "Edge Gateway's environment.",
            parameters = {
                    @Parameter(
                            name = "X-API-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "API key issued to the Edge Gateway",
                            schema = @Schema(
                                    type = "string",
                                    format = "password"
                            )
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Actuation event recorded",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ActuationEventResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Actuation event already registered",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ActuationEventResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid actuation event"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid Gateway API key"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Container monitor not authorized for this Gateway"
            )
    })
    public ResponseEntity<?> recordActuationEvent(
            @PathVariable String gatewayId,
            @PathVariable Long iotDeviceId,

            @AuthenticationPrincipal(expression = "deviceId()")
            String authenticatedGatewayId,

            @AuthenticationPrincipal(expression = "equipmentId()")
            Long gatewayEquipmentId,

            @AuthenticationPrincipal(expression = "laboratoryId()")
            Long laboratoryId,

            @AuthenticationPrincipal(expression = "environmentId()")
            Long environmentId,

            @Valid @RequestBody RecordActuationEventResource resource) {

        // Verify the Gateway identity and its current assignment.
        if (!gatewayId.equals(authenticatedGatewayId)
                || laboratoryId == null
                || environmentId == null) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Verify that the target is a container monitor
        // that the authenticated Gateway serves in its environment.
        var containerMonitor = equipmentService.findContainerMonitor(
                laboratoryId,
                environmentId,
                iotDeviceId
        );

        if (containerMonitor.isEmpty() || !equipmentService.isServedByEdgeGateway(
                laboratoryId, environmentId, gatewayEquipmentId, iotDeviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Reuse the existing Tracking command and validation rules.
        var command = RecordActuationEventCommandFromResourceAssembler
                .toCommandFromResource(
                        laboratoryId,
                        environmentId,
                        iotDeviceId,
                        resource
                );

        return TelemetryResponseAssembler.toRecordedResponse(
                trackingCommandService.handle(command),
                ActuationEventResourceFromEntityAssembler::toResourceFromEntity
        );
    }
}