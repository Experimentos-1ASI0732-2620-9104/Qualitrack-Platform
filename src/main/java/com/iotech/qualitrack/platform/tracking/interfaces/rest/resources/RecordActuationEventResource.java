package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Action executed by a container monitor, synchronized from the Edge.
 */
@Schema(name = "RecordActuationEventRequest", description = "Action executed by the container monitor and its cause")
public record RecordActuationEventResource(
        @Schema(description = "VENTILATION_ON/OFF, COOLING_ON/OFF or SERVO_OPEN/CLOSE", example = "VENTILATION_ON")
        @NotBlank String action,
        @Schema(description = "Metric whose condition caused the action", example = "TEMPERATURE", nullable = true)
        String triggerMetric,
        @Schema(description = "WARNING or CRITICAL that caused the action, or NORMAL when the condition recovered",
                example = "WARNING", nullable = true)
        String triggerState,
        @Schema(description = "EXECUTED (default) or FAILED", example = "EXECUTED", nullable = true)
        String result,
        @Schema(description = "Moment of the action (ISO-8601 with offset)", example = "2026-10-03T15:00:05Z")
        @NotBlank String occurredAt,
        @Schema(description = "Profile version the device applied", example = "3", nullable = true)
        @PositiveOrZero Long profileVersion
) {
}
