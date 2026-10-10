package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Indicators of a laboratory: operational counts and the summary of the environmental readings of a period (US93).
 */
@Schema(name = "KpiDashboardResponse", description = "Indicators calculated from persisted records")
public record KpiDashboardResource(
        @Schema(description = "Snapshot id; null for indicators calculated on request", nullable = true) Long id,
        @Schema(description = "Laboratory", example = "1") Long laboratoryId,
        @Schema(description = "When the indicators were calculated") String timestamp,
        @Schema(description = "Health score; null when no assessment is configured", nullable = true) Double overallHealthScore,
        @Schema(description = "Start of the period of the measurement summaries", nullable = true) Instant from,
        @Schema(description = "End of the period of the measurement summaries", nullable = true) Instant to,
        @Schema(description = "Operational counts of the laboratory") List<KpiMetricResource> metrics,
        @Schema(description = "Average, minimum and maximum per device and metric; empty when there are no readings")
        List<MeasurementSummaryResource> measurementSummaries
) {
}
