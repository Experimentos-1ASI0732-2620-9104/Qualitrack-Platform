package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for assigning the main use of an environment.
 */
@Schema(
        name = "AssignEnvironmentUsageRequest",
        description = "Request payload for assigning the main use of an environment",
        example = "{\"usage\": \"RAW_MATERIAL_STORAGE\"}"
)
public record AssignEnvironmentUsageResource(
        @Schema(description = "Allowed environment usage",
                allowableValues = {"LABORATORY", "PRODUCTION", "RAW_MATERIAL_STORAGE", "PRODUCT_STORAGE", "OTHER"},
                example = "RAW_MATERIAL_STORAGE")
        @NotBlank String usage
) {
}
