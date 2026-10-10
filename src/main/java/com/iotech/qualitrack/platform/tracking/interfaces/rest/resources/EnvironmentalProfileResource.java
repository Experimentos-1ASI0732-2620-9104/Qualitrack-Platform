package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Current configuration of an environment or container monitor, with the version the devices apply.
 */
@Schema(name = "EnvironmentalProfile", description = "Thresholds and actuation rules in force and their version")
public record EnvironmentalProfileResource(
        Long id,
        @Schema(description = "ENVIRONMENT or CONTAINER_MONITOR", example = "CONTAINER_MONITOR") String scope,
        Long laboratoryId,
        @Schema(description = "Environment configured, for the ENVIRONMENT scope", nullable = true) Long environmentId,
        @Schema(description = "Container monitor configured, for the CONTAINER_MONITOR scope", nullable = true) Long deviceId,
        @Schema(description = "Configuration version; it increases with every change", example = "3") long version,
        List<ThresholdResource> thresholds,
        List<ActuationRuleResource> actuationRules,
        @Schema(description = "Moment of the last change (ISO-8601)", nullable = true) String updatedAt,
        @Schema(description = "User that made the last change", nullable = true) Long updatedBy
) {
}
