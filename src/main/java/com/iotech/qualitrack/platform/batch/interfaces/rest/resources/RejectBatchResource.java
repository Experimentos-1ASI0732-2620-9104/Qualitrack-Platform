package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to reject a batch.
 */
@Schema(name = "RejectBatchRequest", description = "Rejection of a product batch with its reason",
        example = "{\"rejectionDate\": \"2026-10-10\", \"reason\": \"Failed final quality control validation\"}")
public record RejectBatchResource(
        @Schema(description = "Rejection date in ISO 8601 format", example = "2026-10-10")
        @NotBlank String rejectionDate,
        @Schema(description = "Reason for rejecting the batch", example = "Failed final quality control validation")
        @NotBlank @Size(max = 500) String reason
) {
}
