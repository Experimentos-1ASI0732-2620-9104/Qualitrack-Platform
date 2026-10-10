package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Equipment or IoT device of a laboratory.
 */
@Schema(name = "EquipmentResponse", description = "Equipment or IoT device registered in a laboratory")
public record EquipmentResource(
        @Schema(description = "Equipment unique numeric identifier", example = "1") Long id,
        @Schema(description = "Laboratory that owns the equipment", example = "1") Long laboratoryId,
        @Schema(description = "Environment where the equipment is located, null until it is associated", example = "2", nullable = true)
        Long environmentId,
        @Schema(description = "Equipment display name", example = "Centrifuge 5000") String name,
        @Schema(description = "Equipment category or type; the device type for IoT devices", example = "Centrifuge") String type,
        @Schema(description = "Manufacturer model", example = "C-5000X") String model,
        @Schema(description = "Unique serial number (MAC or serial for IoT devices)", example = "SN-987654321") String serialNumber,
        @Schema(description = "Current operational status", example = "OPERATIONAL",
                allowableValues = {"OPERATIONAL", "MAINTENANCE", "OUT_OF_SERVICE", "INACTIVE"}) String status,
        @Schema(description = "IoT device type, null for equipment without telemetry", example = "ENVIRONMENTAL_DEVICE",
                allowableValues = {"ENVIRONMENTAL_DEVICE", "CONTAINER_MONITOR"}, nullable = true) String deviceType,
        @Schema(description = "Identifier with which Edge recognises the device", example = "ENV-001", nullable = true)
        String sensorExternalId,
        @Schema(description = "Firmware version of the IoT device", example = "1.0.3", nullable = true) String firmwareVersion
) {
}
