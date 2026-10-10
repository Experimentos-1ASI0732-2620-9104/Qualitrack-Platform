package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Connection state of an IoT device with Edge.
 */
@Schema(name = "DeviceTelemetryStatusResponse", description = "Whether an IoT device is communicating with Edge")
public record DeviceTelemetryStatusResource(
        @Schema(description = "Device (equipment) identifier", example = "7") Long deviceId,
        @Schema(description = "CONNECTED when the device communicated within the expected period, otherwise REQUIRES_REVIEW",
                example = "CONNECTED", allowableValues = {"CONNECTED", "REQUIRES_REVIEW"}) String connectionStatus,
        @Schema(description = "Moment of the latest telemetry or heartbeat received (ISO 8601), null if none",
                example = "2026-10-03T15:30:00Z", nullable = true) String lastCommunicationAt,
        @Schema(description = "Expected period between communications, in seconds", example = "300") long expectedPeriodSeconds
) {
}
