package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Usage assignment registered for an environment.
 */
@Schema(
        name = "EnvironmentUsageAssignmentResponse",
        description = "Usage assigned to an environment",
        example = "{\"environmentId\": 3, \"laboratoryId\": 1, \"usage\": \"RAW_MATERIAL_STORAGE\", \"assignedBy\": 7, \"assignedAt\": \"2026-10-02T15:04:05Z\"}"
)
public record EnvironmentUsageAssignmentResource(
        @Schema(description = "Environment numeric identifier", example = "3")
        Long environmentId,

        @Schema(description = "Laboratory that owns the environment", example = "1")
        Long laboratoryId,

        @Schema(description = "Assigned usage", example = "RAW_MATERIAL_STORAGE")
        String usage,

        @Schema(description = "User that assigned the usage", example = "7")
        Long assignedBy,

        @Schema(description = "Moment of the assignment", example = "2026-10-02T15:04:05Z")
        Instant assignedAt
) {
}
