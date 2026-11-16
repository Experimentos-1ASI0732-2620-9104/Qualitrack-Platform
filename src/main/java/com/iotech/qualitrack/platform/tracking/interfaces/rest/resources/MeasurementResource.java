package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reading of an IoT device and the condition it represents.
 */
@Schema(name = "Measurement", description = "Reading of an IoT device evaluated with the profile in force")
public record MeasurementResource(
        @Schema(description = "Measurement identifier", example = "120") Long id,
        @Schema(description = "Device (equipment) that produced the reading", example = "7") Long deviceId,
        @Schema(description = "Environment where the reading was taken", example = "3") Long environmentId,
        @Schema(description = "Measured metric", example = "TEMPERATURE") String metric,
        @Schema(description = "Numeric value in the unit of the metric; null for an RFID_TAG reading",
                example = "21.5", nullable = true) Double value,
        @Schema(description = "Tag read by an RFID_TAG reading", nullable = true) String textValue,
        @Schema(description = "Unit of the metric (ppm, event, °C, %RH, lux or tag)", example = "°C") String unit,
        @Schema(description = "Moment of the reading (ISO-8601)") String measuredAt,
        @Schema(description = "NORMAL, WARNING or CRITICAL; null when the metric has no threshold", nullable = true) String state,
        @Schema(description = "Limit crossed by a WARNING or CRITICAL reading", nullable = true) Double thresholdValue,
        @Schema(description = "Profile version used to evaluate the reading", nullable = true) Long profileVersion
) {
}
