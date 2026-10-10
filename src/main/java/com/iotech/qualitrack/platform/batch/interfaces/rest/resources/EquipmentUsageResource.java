package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Equipment used in a product batch.
 */
@Schema(name = "EquipmentUsageResponse", description = "Equipment used in a product batch")
public record EquipmentUsageResource(
        @Schema(description = "Usage identifier", example = "1") Long id,
        @Schema(description = "Product batch", example = "1") Long batchId,
        @Schema(description = "Equipment identifier", example = "4") Long equipmentId,
        @Schema(description = "Equipment name when it was associated", example = "Tablet press TP-02") String equipmentName,
        @Schema(description = "User who registered the usage", example = "3") Long registeredByUserId,
        @Schema(description = "Registration time (UTC)", example = "2026-10-02T15:04:05Z") String registeredAt
) {
}
