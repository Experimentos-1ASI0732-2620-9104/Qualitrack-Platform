package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Action executed by a container monitor.
 */
@Schema(name = "ActuationEvent", description = "Action executed by a container monitor and its result")
public record ActuationEventResource(
        Long id,
        Long deviceId,
        Long environmentId,
        @Schema(example = "VENTILATION_ON") String action,
        @Schema(nullable = true) String triggerMetric,
        @Schema(nullable = true) String triggerState,
        @Schema(example = "EXECUTED") String result,
        @Schema(description = "Moment of the action (ISO-8601)") String occurredAt,
        @Schema(nullable = true) Long profileVersion
) {
}
