package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body to register a batch of the product given in the path.
 */
@Schema(name = "CreateBatchRequest", description = "Manufacturing batch to register for the product",
        example = "{\"batchNumber\": \"PB-2026-001\", \"quantity\": 5000, \"unit\": \"units\", \"startDate\": \"2026-10-02\", \"notes\": \"Standard production run\"}")
public record CreateBatchResource(
        @Schema(description = "Batch traceability number, unique in the laboratory", example = "PB-2026-001")
        @NotBlank @Size(max = 50) String batchNumber,
        @Schema(description = "Quantity to produce", example = "5000")
        @NotNull @Positive Double quantity,
        @Schema(description = "Production unit, independent of raw material stock units (defaults to units)", example = "units")
        String unit,
        @Schema(description = "Start date in ISO 8601 format", example = "2026-10-02")
        @NotBlank String startDate,
        @Schema(description = "Optional manufacturing notes", example = "Standard production run", nullable = true)
        @Size(max = 500) String notes
) {
}
