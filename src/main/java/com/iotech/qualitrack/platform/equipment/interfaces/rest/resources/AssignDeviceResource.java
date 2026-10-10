package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body to locate an IoT device of the laboratory in the environment of the path.
 */
@Schema(name = "AssignDeviceRequest", description = "IoT device of the laboratory to locate in the environment")
public record AssignDeviceResource(
        @Schema(description = "Device (equipment) identifier", example = "7") @NotNull @Positive Long deviceId
) {
}
