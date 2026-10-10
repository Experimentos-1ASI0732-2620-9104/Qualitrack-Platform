package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Range of a BPM parameter of an equipment; the parameter is identified by the path.
 *
 * @param minValue minimum acceptable value
 * @param maxValue maximum acceptable value
 * @param unit     measurement unit
 */
@Schema(
        name = "ConfigureBpmRequest",
        description = "Request payload for configuring the limits of a BPM parameter of an equipment",
        example = "{\"minValue\": 2.0, \"maxValue\": 8.0, \"unit\": \"Celsius\"}"
)
public record ConfigureBpmResource(

        @Schema(description = "Minimum acceptable value", example = "2.0")
        Double minValue,

        @Schema(description = "Maximum acceptable value", example = "8.0")
        Double maxValue,

        @Schema(description = "Measurement unit", example = "Celsius", minLength = 1, maxLength = 20)
        String unit
) {
}
