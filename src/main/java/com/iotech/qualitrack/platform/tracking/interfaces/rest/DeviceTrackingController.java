package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.tracking.application.queryservices.TrackingQueryService;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceConnectionQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.DeviceTelemetryStatusResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.EnvironmentalProfileResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Connection state of the IoT devices of an environment and the profile each one applies (US55, TS41, TS57).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/devices/{deviceId}",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class DeviceTrackingController {
    private final TrackingQueryService trackingQueryService;
    private final ExpectedCommunicationPeriod expectedCommunicationPeriod;

    public DeviceTrackingController(TrackingQueryService trackingQueryService,
                                           ExpectedCommunicationPeriod expectedCommunicationPeriod) {
        this.trackingQueryService = trackingQueryService;
        this.expectedCommunicationPeriod = expectedCommunicationPeriod;
    }

    @GetMapping("/telemetry-status")
    @Operation(summary = "Get the connection status of a device",
            description = "CONNECTED when the environmental device or container monitor communicated within the expected period; otherwise REQUIRES_REVIEW.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connection status of the device",
                    content = @Content(schema = @Schema(implementation = DeviceTelemetryStatusResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account"),
            @ApiResponse(responseCode = "404", description = "Not an IoT device located in the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getTelemetryStatus(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                @PathVariable Long deviceId) {
        return trackingQueryService.handle(new GetDeviceConnectionQuery(laboratoryId, environmentId, deviceId))
                .<ResponseEntity<?>>map(connection -> ResponseEntity.ok(new DeviceTelemetryStatusResource(
                        connection.deviceId(), connection.status().name(),
                        connection.lastCommunicationAt() == null ? null : connection.lastCommunicationAt().toString(),
                        expectedCommunicationPeriod.value().toSeconds())))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Device", deviceId)));
    }

    @GetMapping("/environmental-profile")
    @Operation(summary = "Get the profile the device applies",
            description = "Configuration the Edge keeps for the device: the profile of the environment for its environmental "
                    + "device, or the profile of the container monitor, with its version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile in force for the device",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account"),
            @ApiResponse(responseCode = "404", description = "Not an IoT device of the environment, or no profile yet",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getEnvironmentalProfile(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                     @PathVariable Long deviceId) {
        return trackingQueryService.handle(new GetDeviceProfileQuery(laboratoryId, environmentId, deviceId))
                .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(
                        EnvironmentalProfileResourceFromEntityAssembler.toResourceFromEntity(profile)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("EnvironmentalProfile", deviceId)));
    }
}
