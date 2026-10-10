package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body to associate an equipment with the batch in the path.
 */
@Schema(name = "RegisterEquipmentUsageRequest", description = "Equipment of the laboratory used in the batch")
public record RegisterEquipmentUsageResource(
        @Schema(description = "Equipment identifier", example = "4") @NotNull @Positive Long equipmentId
) {
}
