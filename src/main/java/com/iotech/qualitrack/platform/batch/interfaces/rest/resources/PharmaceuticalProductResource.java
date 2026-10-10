package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pharmaceutical product of an environment.
 */
@Schema(name = "PharmaceuticalProductResponse", description = "Pharmaceutical product registered in an environment")
public record PharmaceuticalProductResource(
        @Schema(description = "Product identifier", example = "1") Long id,
        @Schema(description = "Laboratory identifier", example = "1") Long laboratoryId,
        @Schema(description = "Environment where the product is manufactured", example = "2") Long environmentId,
        @Schema(description = "Internal catalog code, unique in the laboratory", example = "PRD-ASP-500") String code,
        @Schema(description = "Product name", example = "Aspirin 500mg") String name,
        @Schema(description = "Product description", example = "Pain reliever and fever reducer") String description,
        @Schema(description = "Technical specifications", example = "Acetylsalicylic acid 500mg, blister pack") String specifications,
        @Schema(description = "Whether the product is active in the catalog", example = "true") boolean active
) {
}
