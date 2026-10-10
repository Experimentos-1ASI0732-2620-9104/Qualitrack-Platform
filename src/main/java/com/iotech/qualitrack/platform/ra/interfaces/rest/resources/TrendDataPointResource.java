package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reading of a deviation trend.
 */
@Schema(name = "TrendDataPointResponse", description = "Reading of the trend")
public record TrendDataPointResource(
        @Schema(description = "When it was measured") String timestamp,
        @Schema(description = "Measured value") Double recordedValue,
        @Schema(description = "Maximum accepted value, when known", nullable = true) Double upperThreshold,
        @Schema(description = "Minimum accepted value, when known", nullable = true) Double lowerThreshold,
        @Schema(description = "NORMAL, WARNING or CRITICAL; null when not evaluated", nullable = true) String state
) {
}
