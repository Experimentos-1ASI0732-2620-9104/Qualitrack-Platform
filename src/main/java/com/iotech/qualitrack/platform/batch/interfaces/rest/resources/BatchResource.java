package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Batch resource.
 */
@Schema(
        name = "BatchResponse",
        description = "Production batch information response",
        example = "{\"id\": 1, \"labId\": 1, \"environmentId\": 2, \"productId\": 10, \"productName\": \"Ibuprofen 400mg\", \"batchNumber\": \"LOT-2026-A\", \"quantity\": 5000.0, \"unit\": \"units\", \"status\": \"PENDING\", \"startDate\": \"2026-05-12\", \"endDate\": null, \"notes\": \"Standard production run\"}"
)
public record BatchResource(

        @Schema(description = "Batch unique numeric identifier", example = "1")
        Long id,

        @Schema(description = "Associated laboratory numeric identifier", example = "1")
        Long labId,
        @Schema(description = "Environment where the batch is manufactured", example = "2", nullable = true)
        Long environmentId,

        @Schema(description = "Associated pharmaceutical product numeric identifier", example = "10")
        Long productId,

        @Schema(description = "Product display name", example = "Ibuprofen 400mg")
        String productName,

        @Schema(description = "Batch traceability number", example = "LOT-2026-A")
        String batchNumber,

        @Schema(description = "Batch quantity", example = "5000.0")
        Double quantity,

        @Schema(description = "Measurement unit", example = "units")
        String unit,

        @Schema(description = "Batch lifecycle status", example = "PENDING")
        String status,

        @Schema(description = "Batch start date", example = "2026-05-12")
        String startDate,

        @Schema(description = "Batch end date", example = "2026-05-20", nullable = true)
        String endDate,

        @Schema(description = "Manufacturing or quality notes", example = "Standard production run", nullable = true)
        String notes,

        @Schema(description = "Container monitor of the container where the batch is stored", example = "12", nullable = true)
        Long containerMonitorId
) {
}