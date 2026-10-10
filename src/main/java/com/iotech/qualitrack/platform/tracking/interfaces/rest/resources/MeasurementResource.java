package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reading of an IoT device and the condition it represents.
 */
@Schema(name = "Measurement", description = "Reading of an IoT device evaluated with the profile in force")
public record MeasurementResource(
        Long id,
        Long deviceId,
        Long environmentId,
        @Schema(example = "TEMPERATURE") String metric,
        @Schema(nullable = true) Double value,
        @Schema(description = "Tag read by an RFID_TAG reading", nullable = true) String textValue,
        @Schema(example = "°C") String unit,
        @Schema(description = "Moment of the reading (ISO-8601)") String measuredAt,
        @Schema(description = "NORMAL, WARNING or CRITICAL; null when the metric has no threshold", nullable = true) String state,
        @Schema(description = "Limit crossed by a WARNING or CRITICAL reading", nullable = true) Double thresholdValue,
        @Schema(description = "Profile version used to evaluate the reading", nullable = true) Long profileVersion
) {
}
