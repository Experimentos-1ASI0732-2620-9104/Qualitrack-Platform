package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body to consume a raw material lot for the batch in the path.
 */
@Schema(name = "RegisterRawMaterialUsageRequest", description = "Raw material lot and amount consumed by the batch",
        example = "{\"rawMaterialBatchId\": 3, \"amountUsed\": 12.5, \"unit\": \"kg\", \"operationId\": \"7f1c2e0a-5b7d-4c1e-9a55-2f0c8f6d1b20\"}")
public record RegisterRawMaterialUsageResource(
        @Schema(description = "Inventory raw material lot to consume", example = "3")
        @NotNull @Positive Long rawMaterialBatchId,
        @Schema(description = "Amount consumed", example = "12.5")
        @NotNull @Positive BigDecimal amountUsed,
        @Schema(description = "Unit of the amount, compatible with the lot unit", example = "kg")
        @NotBlank String unit,
        @Schema(description = "Idempotency key; retrying with the same key does not consume twice", example = "7f1c2e0a-5b7d-4c1e-9a55-2f0c8f6d1b20")
        @NotBlank @Size(max = 100) String operationId
) {
}
