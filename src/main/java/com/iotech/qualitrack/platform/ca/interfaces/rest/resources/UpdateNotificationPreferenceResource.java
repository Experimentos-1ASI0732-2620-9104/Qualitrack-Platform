package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UpdateNotificationPreferenceRequest",
        description = "Request payload for updating notification preferences",
        example = "{\"emailEnabled\": true, \"inAppEnabled\": true, \"minimumSeverity\": \"WARNING\"}"
)
public record UpdateNotificationPreferenceResource(
        @Schema(description = "Whether email notifications are enabled", example = "true")
        Boolean emailEnabled,

        @Schema(description = "Whether in-app notifications are enabled", example = "true")
        Boolean inAppEnabled,

        @Schema(description = "Minimum severity required to notify the user", example = "WARNING")
        AlertSeverity minimumSeverity
) {
    public UpdateNotificationPreferenceResource {
        if (emailEnabled == null) {
            throw new IllegalArgumentException("emailEnabled cannot be null");
        }
        if (inAppEnabled == null) {
            throw new IllegalArgumentException("inAppEnabled cannot be null");
        }
        if (minimumSeverity == null) {
            throw new IllegalArgumentException("minimumSeverity cannot be null");
        }
    }
}