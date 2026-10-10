package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register a change in the operational status of an equipment.
 */
@Schema(name = "ChangeEquipmentStatusRequest", description = "Operational status to register for the equipment")
public record ChangeEquipmentStatusResource(
        @Schema(description = "New operational status", example = "MAINTENANCE",
                allowableValues = {"OPERATIONAL", "MAINTENANCE", "OUT_OF_SERVICE", "INACTIVE"})
        @NotBlank String status,
        @Schema(description = "Optional reason for the change", example = "Scheduled calibration", nullable = true)
        @Size(max = 500) String reason
) {
}
