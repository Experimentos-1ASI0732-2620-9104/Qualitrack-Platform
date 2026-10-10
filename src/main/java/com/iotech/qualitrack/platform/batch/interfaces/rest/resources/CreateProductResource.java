package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register a pharmaceutical product in an environment.
 */
@Schema(name = "CreateProductRequest", description = "Pharmaceutical product to register in the environment")
public record CreateProductResource(
        @Schema(description = "Internal catalog code, unique in the laboratory", example = "PRD-ASP-500")
        @NotBlank @Size(max = 50) String code,
        @Schema(description = "Product name, unique in the laboratory", example = "Aspirin 500mg")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Optional description", example = "Pain reliever and fever reducer")
        @Size(max = 500) String description,
        @Schema(description = "Technical specifications", example = "Acetylsalicylic acid 500mg, blister pack")
        @NotBlank @Size(max = 1000) String specifications
) {
}
