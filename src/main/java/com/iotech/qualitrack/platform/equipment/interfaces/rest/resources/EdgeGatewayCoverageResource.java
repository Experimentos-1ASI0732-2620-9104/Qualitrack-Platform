package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * How the Edge Gateways of an environment cover its IoT devices.
 *
 * <p>Web and mobile read it to warn the user when devices of the environment have no Gateway; the rule that
 * distributes the devices lives only in the platform.</p>
 *
 * @param maxDevicesPerGateway number of IoT devices one Edge Gateway serves at most
 * @param deviceCount IoT devices located in the environment
 * @param uncoveredDeviceCount IoT devices of the environment that no Gateway serves
 * @param gateways Edge Gateways of the environment, in the order in which they receive devices
 */
@Schema(name = "EdgeGatewayCoverage", description = "Coverage of the IoT devices of an environment by its Edge Gateways")
public record EdgeGatewayCoverageResource(
        @Schema(description = "IoT devices one Edge Gateway serves at most", example = "15")
        int maxDevicesPerGateway,

        @Schema(description = "IoT devices located in the environment", example = "20")
        int deviceCount,

        @Schema(description = "IoT devices of the environment without an Edge Gateway", example = "5")
        int uncoveredDeviceCount,

        @Schema(description = "Edge Gateways of the environment and the devices each one serves")
        List<Gateway> gateways
) {

    /**
     * An Edge Gateway of the environment and its share of the devices.
     *
     * @param equipmentId equipment identifier of the Gateway
     * @param name Gateway display name
     * @param sensorExternalId external identifier the Gateway authenticates with
     * @param deviceCount IoT devices the Gateway serves
     */
    @Schema(name = "EdgeGatewayCoverageGateway", description = "Edge Gateway of the environment and the devices it serves")
    public record Gateway(
            @Schema(description = "Equipment identifier of the Gateway", example = "30")
            Long equipmentId,

            @Schema(description = "Gateway display name", example = "Edge Gateway - Production")
            String name,

            @Schema(description = "External Gateway identifier", example = "EDGE-001")
            String sensorExternalId,

            @Schema(description = "IoT devices served by the Gateway", example = "15")
            int deviceCount
    ) {
    }
}
