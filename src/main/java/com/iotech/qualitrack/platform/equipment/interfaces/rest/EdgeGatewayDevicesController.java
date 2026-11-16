package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByEnvironmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.services.EdgeGatewayAllocation;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EdgeGatewayIotDeviceResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for Edge Gateway device registry synchronization.
 *
 * <p>Exposes only the IoT devices that the authenticated Edge
 * Gateway serves: its share of the devices of its environment,
 * {@value EdgeGatewayAllocation#MAX_DEVICES_PER_EDGE_GATEWAY} at most
 * (see {@link EdgeGatewayAllocation}).</p>
 *
 * <p>The laboratory and environment identifiers are obtained
 * from the authenticated Gateway principal instead of
 * client-supplied request parameters.</p>
 */
@RestController
@RequestMapping(
        value = "/api/v1/edge-gateways",
        produces = APPLICATION_JSON_VALUE
)
@PreAuthorize("hasAuthority('ROLE_EDGE_GATEWAY')")
public class EdgeGatewayDevicesController {

    private final EquipmentQueryService equipmentQueryService;

    public EdgeGatewayDevicesController(
            EquipmentQueryService equipmentQueryService) {

        this.equipmentQueryService = equipmentQueryService;
    }

    /**
     * Retrieves the IoT devices that the authenticated Edge
     * Gateway serves in its environment.
     *
     * <p>Only the equipment of the environment is read. When the
     * environment has several Gateways, or more devices than one
     * Gateway can serve, each Gateway receives its own share and
     * never the devices of another Gateway.</p>
     *
     * <p>The response includes environmental devices and
     * container monitors, excluding ordinary equipment and
     * Edge Gateways.</p>
     *
     * <p>Device status is preserved to allow Edge Service
     * to distinguish operational and inactive devices.</p>
     *
     * @param deviceId external identifier from the request path
     * @param authenticatedDeviceId authenticated Gateway identifier
     * @param laboratoryId laboratory of the authenticated Gateway
     * @param environmentId environment assigned to the Gateway
     * @return IoT devices served by the Gateway, by external identifier
     */
    @GetMapping("/{deviceId}/iot-devices")
    @Operation(
            summary = "Get Edge Gateway IoT devices",
            description = "Retrieves the environmental devices and container "
                    + "monitors that the authenticated Gateway serves in its "
                    + "current environment: 15 at most. With several Gateways "
                    + "in the environment, each one receives the next 15 devices "
                    + "by identifier.",
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
                    responseCode = "200",
                    description = "IoT devices retrieved successfully",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation =
                                                    EdgeGatewayIotDeviceResource.class
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid Gateway API key"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Gateway is not authorized for this resource, "
                            + "or is no longer located in its environment"
            )
    })
    public List<EdgeGatewayIotDeviceResource> getAssignedDevices(
            @PathVariable String deviceId,

            @AuthenticationPrincipal(expression = "deviceId()")
            String authenticatedDeviceId,

            @AuthenticationPrincipal(expression = "laboratoryId()")
            Long laboratoryId,

            @AuthenticationPrincipal(expression = "environmentId()")
            Long environmentId) {

        if (!deviceId.equals(authenticatedDeviceId)
                || laboratoryId == null
                || environmentId == null) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Gateway is not authorized for this environment"
            );
        }

        var environmentEquipment = equipmentQueryService.handle(
                        new GetEquipmentByEnvironmentIdQuery(
                                laboratoryId,
                                environmentId,
                                false
                        )
                )
                .orElseGet(List::of);

        var gateway = environmentEquipment.stream()
                .filter(Equipment::isEdgeGateway)
                .filter(equipment ->
                        equipment.getSensorExternalId() != null
                                && deviceId.equals(
                                equipment.getSensorExternalId().value())
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Gateway is not authorized for this environment"
                ));

        // The Gateway serves only its share of the environment (15 devices at most).
        var ownDeviceIds = Set.copyOf(
                EdgeGatewayAllocation.ofEnvironment(environmentEquipment)
                        .deviceIdsOf(gateway.getId())
        );

        return environmentEquipment.stream()
                .filter(equipment ->
                        ownDeviceIds.contains(equipment.getId())
                )
                .sorted(
                        Comparator.comparing(
                                equipment ->
                                        equipment.getSensorExternalId().value()
                        )
                )
                .map(equipment -> new EdgeGatewayIotDeviceResource(
                        equipment.getId(),
                        equipment.getLabId(),
                        equipment.getEnvironmentId(),
                        equipment.getName(),
                        equipment.getSensorExternalId().value(),
                        equipment.getDeviceType().name(),
                        equipment.getStatus().name()
                ))
                .toList();
    }
}