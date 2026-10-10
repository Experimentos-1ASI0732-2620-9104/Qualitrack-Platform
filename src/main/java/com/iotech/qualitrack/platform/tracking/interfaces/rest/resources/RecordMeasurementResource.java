package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Reading synchronized from the Edge.
 */
@Schema(name = "RecordMeasurementRequest", description = "Reading of the environmental device or of a container monitor")
public record RecordMeasurementResource(
        @Schema(description = "AIR_QUALITY or MOTION for the environmental device; TEMPERATURE, HUMIDITY, LUMINOSITY or RFID_TAG for a container monitor",
                example = "TEMPERATURE")
        @NotBlank String metric,
        @Schema(description = "Numeric value in the unit of the metric; MOTION is 1 when detected and 0 otherwise",
                example = "21.5", nullable = true)
        Double value,
        @Schema(description = "Tag read, only for RFID_TAG", example = "E200-3412-0001", nullable = true)
        String textValue,
        @Schema(description = "Moment of the reading (ISO-8601 with offset)", example = "2026-10-03T15:00:00Z")
        @NotBlank String measuredAt,
        @Schema(description = "Profile version the device applied, when it reports it", example = "3", nullable = true)
        @PositiveOrZero Long profileVersion
) {
}
