package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Average, minimum and maximum of the readings of a device and metric in the period of the indicators (US93).
 */
@Schema(name = "MeasurementSummaryResponse", description = "Summary of the readings of a device and metric")
public record MeasurementSummaryResource(
        @Schema(description = "Environment of the device", example = "4") Long environmentId,
        @Schema(description = "Environmental device or container monitor", example = "12") Long deviceId,
        @Schema(description = "Measured metric", example = "TEMPERATURE") String metric,
        @Schema(description = "Unit of the values", example = "°C") String unit,
        @Schema(description = "Readings summarized", example = "288") int readings,
        @Schema(description = "Arithmetic mean", example = "21.4") Double average,
        @Schema(description = "Lowest value", example = "18.2") Double minimum,
        @Schema(description = "Highest value", example = "27.9") Double maximum,
        @Schema(description = "First reading of the period") Instant firstMeasuredAt,
        @Schema(description = "Last reading of the period") Instant lastMeasuredAt
) {
}
