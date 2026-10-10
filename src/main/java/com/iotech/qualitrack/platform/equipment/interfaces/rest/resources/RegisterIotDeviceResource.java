package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register an ESP32 environmental device or container monitor; the type comes from the path.
 */
@Schema(name = "RegisterIotDeviceRequest", description = "ESP32 device to register with the identity Edge uses to recognise it")
public record RegisterIotDeviceResource(
        @Schema(description = "Device display name", example = "Space monitor - Raw material warehouse")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Unique identifier with which Edge recognises the device", example = "ENV-001")
        @NotBlank @Size(max = 50) String sensorExternalId,
        @Schema(description = "Unique MAC address or serial number", example = "24:6F:28:AA:10:01")
        @NotBlank @Size(max = 50) String serialNumber,
        @Schema(description = "Hardware model", example = "ESP32-WROOM-32")
        @NotBlank @Size(max = 100) String model,
        @Schema(description = "Firmware version", example = "1.0.3", nullable = true)
        @Size(max = 50) String firmwareVersion
) {
}
