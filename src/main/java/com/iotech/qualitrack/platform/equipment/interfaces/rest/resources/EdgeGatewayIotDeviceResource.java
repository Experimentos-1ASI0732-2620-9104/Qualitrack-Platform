package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IoT device information exposed to an authenticated Edge Gateway.
 *
 * <p>Contains the identifiers and operational information
 * required by Edge Service to synchronize its local device
 * registry.</p>
 *
 * <p>Authentication credentials are not included because
 * the ESP32 devices authenticate locally against Edge Service.</p>
 *
 * @param id equipment identifier
 * @param laboratoryId owning laboratory identifier
 * @param environmentId assigned environment identifier
 * @param name device display name
 * @param sensorExternalId external identifier recognized by Edge
 * @param deviceType type of IoT device
 * @param status current operational status
 */
@Schema(name = "EdgeGatewayIotDevice", description = "IoT device assigned to the authenticated Edge Gateway environment")
public record EdgeGatewayIotDeviceResource(

        @Schema(description = "Equipment identifier", example = "21")
        Long id,

        @Schema(description = "Laboratory identifier", example = "1")
        Long laboratoryId,

        @Schema(description = "Environment identifier", example = "3")
        Long environmentId,

        @Schema(description = "Device display name", example = "Environmental Monitor")
        String name,

        @Schema(description = "External device identifier", example = "ENV-001")
        String sensorExternalId,

        @Schema(description = "IoT device type", example = "ENVIRONMENTAL_DEVICE",
                allowableValues = {"ENVIRONMENTAL_DEVICE", "CONTAINER_MONITOR"}
        )
        String deviceType,

        @Schema(description = "Current operational status", example = "OPERATIONAL",
                allowableValues = {"OPERATIONAL", "MAINTENANCE", "OUT_OF_SERVICE", "INACTIVE"}
        )
        String status
) {}
