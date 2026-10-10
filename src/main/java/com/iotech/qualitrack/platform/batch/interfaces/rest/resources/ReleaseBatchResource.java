package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to release a batch.
 */
@Schema(name = "ReleaseBatchRequest", description = "Release of a product batch after quality review",
        example = "{\"releaseDate\": \"2026-10-10\", \"notes\": \"All quality controls passed\"}")
public record ReleaseBatchResource(
        @Schema(description = "Release date in ISO 8601 format", example = "2026-10-10")
        @NotBlank String releaseDate,
        @Schema(description = "Final quality remarks", example = "All quality controls passed")
        @NotBlank @Size(max = 500) String notes
) {
}
