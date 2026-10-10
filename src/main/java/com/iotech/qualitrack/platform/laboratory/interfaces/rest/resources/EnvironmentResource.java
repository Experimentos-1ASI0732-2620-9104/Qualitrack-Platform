package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Environment information returned by the API.
 */
@Schema(
        name = "EnvironmentResponse",
        description = "Environment registered inside a laboratory",
        example = "{\"id\": 3, \"laboratoryId\": 1, \"code\": \"WH-RM-01\", \"name\": \"Raw material warehouse\", \"description\": \"Main storage room\", \"usage\": \"RAW_MATERIAL_STORAGE\", \"usageAssignedBy\": 7, \"usageAssignedAt\": \"2026-10-02T15:04:05Z\"}"
)
public record EnvironmentResource(
        @Schema(description = "Environment numeric identifier", example = "3")
        Long id,

        @Schema(description = "Laboratory that owns the environment", example = "1")
        Long laboratoryId,

        @Schema(description = "Identification of the environment, unique within the laboratory", example = "WH-RM-01")
        String code,

        @Schema(description = "Display name", example = "Raw material warehouse")
        String name,

        @Schema(description = "Optional description", example = "Main storage room")
        String description,

        @Schema(description = "Main use of the environment; null until assigned", example = "RAW_MATERIAL_STORAGE", nullable = true)
        String usage,

        @Schema(description = "User that assigned the current usage", example = "7", nullable = true)
        Long usageAssignedBy,

        @Schema(description = "Moment when the current usage was assigned", example = "2026-10-02T15:04:05Z", nullable = true)
        Instant usageAssignedAt
) {
}
