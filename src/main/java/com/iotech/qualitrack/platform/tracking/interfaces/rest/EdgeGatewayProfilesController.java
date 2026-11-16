package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.tracking.application.queryservices.TrackingQueryService;
import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceProfileQuery;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.EnvironmentalProfileResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Exposes IoT environmental profiles to authenticated Edge Gateways, so the Edge applies the same thresholds and
 * actuation rules the platform uses (TS57).
 *
 * <p>The Gateway authenticates with its API key ({@code X-API-Key}) and gets the {@code ROLE_EDGE_GATEWAY}
 * authority. The laboratory and environment are obtained from the authenticated Gateway, never from user-supplied
 * parameters, so a Gateway can only read the profiles of the devices it serves in its own environment.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/edge-gateways", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('ROLE_EDGE_GATEWAY')")
public class EdgeGatewayProfilesController {

    private final TrackingQueryService trackingQueryService;
    private final TrackingExternalEquipmentService equipmentService;

    public EdgeGatewayProfilesController(
            TrackingQueryService trackingQueryService,
            TrackingExternalEquipmentService equipmentService) {

        this.trackingQueryService = trackingQueryService;
        this.equipmentService = equipmentService;
    }

    /**
     * Retrieves the environmental profile applied by an IoT device.
     *
     * <p>Environmental devices use their environment profile,
     * while container monitors use their own profile.</p>
     *
     * @param gatewayId external Gateway identifier
     * @param iotDeviceId numeric Equipment identifier
     * @param authenticatedGatewayId authenticated Gateway identifier
     * @param gatewayEquipmentId Equipment identifier of the authenticated Gateway
     * @param laboratoryId authenticated laboratory
     * @param environmentId authenticated environment
     * @return 200 with the profile, 404 when no profile exists, or 403 when the Gateway in the path is not the
     *         authenticated one, it has no current assignment or it does not serve the device in its environment
     */
    @GetMapping("/{gatewayId}/iot-devices/{iotDeviceId}/environmental-profile")
    @Operation(summary = "Get IoT environmental profile",
            description = "Returns the current environmental profile "
                    + "of an IoT device assigned to the authenticated Gateway.",
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
            @ApiResponse(responseCode = "200", description = "Profile in force for the device",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Gateway API key"),
            @ApiResponse(responseCode = "403", description = "Gateway cannot read the profile of this IoT device"),
            @ApiResponse(responseCode = "404", description = "The device has no profile yet")
    })
    public ResponseEntity<?> getProfile(
            @PathVariable String gatewayId,
            @PathVariable Long iotDeviceId,

            @AuthenticationPrincipal(expression = "deviceId()")
            String authenticatedGatewayId,

            @AuthenticationPrincipal(expression = "equipmentId()")
            Long gatewayEquipmentId,

            @AuthenticationPrincipal(expression = "laboratoryId()")
            Long laboratoryId,

            @AuthenticationPrincipal(expression = "environmentId()")
            Long environmentId) {

        if (!gatewayId.equals(authenticatedGatewayId)
                || laboratoryId == null
                || environmentId == null) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Only the profile of a device this Gateway serves in its environment.
        var device = equipmentService.findDevice(
                laboratoryId,
                environmentId,
                iotDeviceId
        );

        if (device.isEmpty() || !equipmentService.isServedByEdgeGateway(
                laboratoryId, environmentId, gatewayEquipmentId, iotDeviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return trackingQueryService.handle(
                        new GetDeviceProfileQuery(
                                laboratoryId,
                                environmentId,
                                iotDeviceId
                        )
                )
                .<ResponseEntity<?>>map(profile ->
                        ResponseEntity.ok(
                                EnvironmentalProfileResourceFromEntityAssembler
                                        .toResourceFromEntity(profile)
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }
}