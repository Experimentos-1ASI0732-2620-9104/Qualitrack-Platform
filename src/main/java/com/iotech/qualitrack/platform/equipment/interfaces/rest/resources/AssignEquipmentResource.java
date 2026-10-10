package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body to locate an equipment of the laboratory in the environment of the path.
 */
@Schema(name = "AssignEquipmentRequest", description = "Equipment of the laboratory to locate in the environment")
public record AssignEquipmentResource(
        @Schema(description = "Equipment identifier", example = "5") @NotNull @Positive Long equipmentId
) {
}
