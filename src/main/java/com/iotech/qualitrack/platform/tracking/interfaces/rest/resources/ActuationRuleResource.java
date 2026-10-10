package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Condition of the container and the automatic action the container monitor executes.
 */
@Schema(name = "ActuationRule", description = "When the metric reaches the state, the container monitor executes the action")
public record ActuationRuleResource(
        @Schema(description = "TEMPERATURE, HUMIDITY or LUMINOSITY; it needs a threshold in the profile", example = "TEMPERATURE")
        @NotBlank String metric,
        @Schema(description = "WARNING or CRITICAL", example = "WARNING")
        @NotBlank String state,
        @Schema(description = "VENTILATION_ON, COOLING_ON or SERVO_OPEN", example = "VENTILATION_ON")
        @NotBlank String action
) {
}
