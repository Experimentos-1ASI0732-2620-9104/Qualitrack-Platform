package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Notification", description = "Notice of something that happened to an alert or a batch of the laboratory. "
        + "Clients compose the text in the language of the person from the type and the values.")
public record NotificationResource(
        Long id,
        @Schema(description = "ALERT_OPENED, ALERT_ESCALATED, ALERT_ACKNOWLEDGED, ALERT_RESOLVED, BATCH_RELEASED or BATCH_REJECTED")
        String type,
        @Schema(description = "Severity of the alert (WARNING or CRITICAL); null for a batch") String severity,
        @Schema(description = "ALERT or BATCH") String subjectType,
        @Schema(description = "The alert or the batch") Long subjectId,
        @Schema(description = "Environment of the alert") String environmentName,
        @Schema(description = "Device of the alert or number of the batch") String subjectName,
        @Schema(description = "Variable of the alert, for example TEMPERATURE") String parameterName,
        @Schema(description = "Value that deviated") Double recordedValue,
        @Schema(description = "Unit of the value") String unit,
        @Schema(description = "Person who acknowledged, resolved, released or rejected") String actorName,
        @Schema(description = "Rejection reason or resolution notes") String note,
        @Schema(description = "When it happened (ISO-8601)") String occurredAt,
        @Schema(description = "When the person read it (ISO-8601); null while unread") String readAt
) {
}
