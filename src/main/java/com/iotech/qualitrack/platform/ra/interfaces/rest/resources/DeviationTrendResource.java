package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.TrendDirection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Deviation indicators of a variable of a device in a period (US94).
 */
@Schema(name = "DeviationTrendResponse", description = "Time in range, deviations and readings of a variable",
        externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(description =
                "Time in range: each evaluated reading keeps its condition until the next one; NORMAL time over the "
                        + "time between the first and last evaluated reading. Deviations: readings worse than the previous one."))
public record DeviationTrendResource(
        @Schema(description = "Trend id; null for indicators calculated on request", nullable = true) Long id,
        @Schema(description = "Monitored variable", example = "TEMPERATURE") String parameterName,
        @Schema(description = "Environmental device or container monitor", example = "12") Long equipmentId,
        @Schema(description = "Environment of the device", example = "4", nullable = true) Long environmentId,
        @Schema(description = "Unit of the readings", example = "°C", nullable = true) String unit,
        @Schema(description = "INCREASING, DECREASING or STABLE (first and last value)") TrendDirection trendDirection,
        @Schema(description = "Readings evaluated against the thresholds", example = "250") int evaluatedReadings,
        @Schema(description = "Percentage of time in NORMAL; null with fewer than two evaluated readings", example = "93.5", nullable = true)
        Double timeInRangePercent,
        @Schema(description = "Readings that moved the variable to a worse condition", example = "3") int deviationCount,
        @Schema(description = "Readings that moved the variable to CRITICAL", example = "1") int criticalDeviationCount,
        @Schema(description = "Readings of the period in time order") List<TrendDataPointResource> dataPoints
) {
}
