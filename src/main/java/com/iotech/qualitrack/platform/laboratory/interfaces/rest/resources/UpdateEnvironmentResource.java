package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the identification data of an environment.
 */
@Schema(
        name = "UpdateEnvironmentRequest",
        description = "Request payload for updating an existing environment",
        example = "{\"code\": \"WH-RM-01\", \"name\": \"Raw material warehouse A\", \"description\": \"Cold storage for incoming raw materials\"}"
)
public record UpdateEnvironmentResource(
        @Schema(description = "Identification of the environment, unique within the laboratory", example = "WH-RM-01", maxLength = 30)
        @NotBlank @Size(max = 30) String code,

        @Schema(description = "Display name of the environment", example = "Raw material warehouse A", maxLength = 100)
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "Optional description of the environment", example = "Cold storage for incoming raw materials", maxLength = 255)
        @Size(max = 255) String description
) {
}
