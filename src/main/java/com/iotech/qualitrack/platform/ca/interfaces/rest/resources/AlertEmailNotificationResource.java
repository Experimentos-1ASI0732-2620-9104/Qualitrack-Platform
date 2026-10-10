package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlertEmailNotification", description = "E-mail notice of a critical alert sent to the laboratory (TS78)")
public record AlertEmailNotificationResource(
        Long alertId,
        @Schema(description = "People who enabled e-mail notices and have an e-mail") int recipients,
        @Schema(description = "E-mails the provider accepted") int delivered,
        @Schema(description = "When it was sent (ISO-8601)") String sentAt
) {
}
