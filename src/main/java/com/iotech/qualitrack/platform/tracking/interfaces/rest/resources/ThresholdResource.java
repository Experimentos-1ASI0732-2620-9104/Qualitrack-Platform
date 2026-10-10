package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * WARNING/CRITICAL limits of a metric. Each side of the range needs both its normal and its critical value or none.
 */
@Schema(name = "EnvironmentalThreshold",
        description = "NORMAL inside [normalMin, normalMax], WARNING up to the critical limits, CRITICAL beyond them")
public record ThresholdResource(
        @Schema(description = "AIR_QUALITY for an environment; TEMPERATURE, HUMIDITY or LUMINOSITY for a container monitor",
                example = "TEMPERATURE")
        @NotBlank String metric,
        @Schema(description = "Unit of the metric, set by the platform (ppm, °C, %RH, lux)", example = "°C",
                accessMode = Schema.AccessMode.READ_ONLY)
        String unit,
        @Schema(description = "Lowest NORMAL value; null when the metric has no lower limit", example = "15", nullable = true)
        Double normalMin,
        @Schema(description = "Highest NORMAL value; null when the metric has no upper limit", example = "25", nullable = true)
        Double normalMax,
        @Schema(description = "Lowest WARNING value; lower values are CRITICAL", example = "8", nullable = true)
        Double criticalMin,
        @Schema(description = "Highest WARNING value; higher values are CRITICAL", example = "30", nullable = true)
        Double criticalMax
) {
}
