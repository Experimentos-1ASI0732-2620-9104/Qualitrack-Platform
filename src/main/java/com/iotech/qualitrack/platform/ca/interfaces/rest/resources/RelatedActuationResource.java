package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Action executed by a container monitor while the incident of an alert was open.
 */
@Schema(name = "RelatedActuationResponse", description = "Action related to the alert")
public record RelatedActuationResource(
        @Schema(description = "Tracking actuation event", example = "40") Long id,
        @Schema(description = "Executed action", example = "COOLING_ON") String action,
        @Schema(description = "Condition that triggered it", example = "CRITICAL", nullable = true) String triggerState,
        @Schema(description = "EXECUTED or FAILED", example = "EXECUTED") String result,
        @Schema(description = "When the device executed it") Instant occurredAt
) {
}
