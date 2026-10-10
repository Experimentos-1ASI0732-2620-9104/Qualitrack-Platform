package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Deviation alert with the actions its container monitor executed for the same variable during the incident (US86).
 */
@Schema(name = "DeviationAlertDetailResponse", description = "Deviation alert, its origin and the related actions")
public record DeviationAlertDetailResource(
        @Schema(description = "Alert", example = "15") Long id,
        @Schema(description = "Laboratory; null for alerts registered before environments", example = "1", nullable = true) Long laboratoryId,
        @Schema(description = "Environment where the deviation was detected", example = "4", nullable = true) Long environmentId,
        @Schema(description = "ENVIRONMENT (environmental device) or CONTAINER (container monitor)", example = "CONTAINER", nullable = true) AlertOrigin origin,
        @Schema(description = "Environmental device, container monitor or equipment that detected it", example = "12") Long equipmentId,
        @Schema(description = "Product batch, when the alert concerns one", nullable = true) Long batchId,
        @Schema(description = "Tracking measurement that opened the incident", example = "301", nullable = true) Long measurementId,
        @Schema(description = "Tracking measurement of the latest deviation", example = "320", nullable = true) Long lastMeasurementId,
        @Schema(description = "Monitored variable", example = "TEMPERATURE") String parameterName,
        @Schema(description = "Value of the deviation that set the current severity", example = "33.0") Double recordedValue,
        @Schema(description = "Limit crossed by that value", example = "30.0") Double thresholdValue,
        @Schema(description = "Unit of the value and the limit", example = "°C") String unit,
        @Schema(description = "When the incident started", example = "2026-10-04T14:05:00Z") String timestamp,
        @Schema(description = "Highest severity reached by the incident", example = "CRITICAL") AlertSeverity severity,
        @Schema(description = "UNRESOLVED, ACKNOWLEDGED or RESOLVED", example = "UNRESOLVED") AlertStatus status,
        @Schema(description = "Deviations correlated with the incident", example = "3") Integer deviationCount,
        @Schema(description = "When the latest deviation was measured", nullable = true) Instant lastDetectedAt,
        @Schema(description = "When the condition returned to normal; null while it is deviated", nullable = true) Instant normalizedAt,
        @Schema(description = "User attending the alert", nullable = true) Long acknowledgedBy,
        @Schema(description = "When the attention started", nullable = true) Instant acknowledgedAt,
        @Schema(description = "User who resolved the alert", nullable = true) Long resolvedBy,
        @Schema(description = "When the alert was resolved", nullable = true) Instant resolvedAt,
        @Schema(description = "Corrective action or resolution notes", nullable = true) String resolutionNotes,
        @Schema(description = "Actions executed by the container monitor for the variable while the incident was open")
        List<RelatedActuationResource> relatedActuations
) {
}
