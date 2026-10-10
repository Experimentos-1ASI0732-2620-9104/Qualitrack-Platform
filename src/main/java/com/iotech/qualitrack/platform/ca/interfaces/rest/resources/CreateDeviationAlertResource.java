package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Deviation confirmed for an environment or one of its monitored containers (TS73).
 */
@Schema(name = "CreateDeviationAlertRequest", description = "Deviation of an environment or container",
        example = "{\"deviceId\": 12, \"parameterName\": \"TEMPERATURE\", \"recordedValue\": 33.0, \"thresholdValue\": 30.0, "
                + "\"unit\": \"°C\", \"severity\": \"CRITICAL\", \"detectedAt\": \"2026-10-04T14:05:00Z\"}")
public record CreateDeviationAlertResource(
        @Schema(description = "Environmental device or container monitor located in the environment; omitted means the "
                + "environmental device of the environment", example = "12", nullable = true)
        Long deviceId,
        @Schema(description = "Monitored variable", example = "TEMPERATURE") String parameterName,
        @Schema(description = "Measured value", example = "33.0") Double recordedValue,
        @Schema(description = "Limit crossed by the value", example = "30.0") Double thresholdValue,
        @Schema(description = "Unit of the value and the limit", example = "°C") String unit,
        @Schema(description = "LOW, WARNING or CRITICAL", example = "CRITICAL") String severity,
        @Schema(description = "When the deviation was measured", example = "2026-10-04T14:05:00Z") Instant detectedAt
) {
}
