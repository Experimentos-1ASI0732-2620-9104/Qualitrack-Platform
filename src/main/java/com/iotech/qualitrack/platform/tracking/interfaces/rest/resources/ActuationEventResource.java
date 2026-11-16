package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Action executed by a container monitor.
 */
@Schema(name = "ActuationEvent", description = "Action executed by a container monitor and its result")
public record ActuationEventResource(
        @Schema(description = "Actuation event identifier", example = "15") Long id,
        @Schema(description = "Container monitor (equipment) that executed the action", example = "7") Long deviceId,
        @Schema(description = "Environment where the container monitor is located", example = "3") Long environmentId,
        @Schema(description = "VENTILATION_ON/OFF, COOLING_ON/OFF or SERVO_OPEN/CLOSE", example = "VENTILATION_ON")
        String action,
        @Schema(description = "Metric whose condition caused the action; null when the device reported no cause",
                example = "TEMPERATURE", nullable = true) String triggerMetric,
        @Schema(description = "WARNING or CRITICAL that caused the action, or NORMAL when the condition recovered",
                example = "WARNING", nullable = true) String triggerState,
        @Schema(description = "EXECUTED or FAILED", example = "EXECUTED") String result,
        @Schema(description = "Moment of the action (ISO-8601)", example = "2026-10-03T15:00:05Z") String occurredAt,
        @Schema(description = "Profile version the device applied", example = "3", nullable = true) Long profileVersion
) {
}
