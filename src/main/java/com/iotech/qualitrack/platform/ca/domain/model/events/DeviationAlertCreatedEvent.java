package com.iotech.qualitrack.platform.ca.domain.model.events;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Domain event published when a deviation alert is created.
 *
 * <p>Can be listened to by batch, reporting, audit, or notification contexts to
 * react to a new compliance deviation without directly coupling to the CA
 * application services.</p>
 *
 * @param alertId The numeric identity assigned to the deviation alert.
 * @param equipmentId The numeric identity of the equipment that generated the alert.
 * @param batchId The numeric identity of the related batch, if applicable.
 * @param parameterName The monitored parameter that deviated.
 * @param recordedValue The measured value that triggered the alert.
 * @param thresholdValue The configured threshold value.
 * @param unit The measurement unit.
 * @param timestamp The timestamp when the deviation occurred.
 * @param severity The severity level of the deviation alert.
 */
public record DeviationAlertCreatedEvent(
        Long alertId,
        Long laboratoryId,
        Long environmentId,
        AlertOrigin origin,
        Long equipmentId,
        Long batchId,
        String parameterName,
        Double recordedValue,
        Double thresholdValue,
        String unit,
        String timestamp,
        AlertSeverity severity
) {
    /**
     * Convenience factory that extracts all needed fields from a saved {@link DeviationAlert}.
     *
     * @param alert the saved deviation alert
     * @return a fully populated {@link DeviationAlertCreatedEvent}
     */
    public static DeviationAlertCreatedEvent from(DeviationAlert alert) {
        return new DeviationAlertCreatedEvent(
                alert.getId(),
                alert.getLaboratoryId(),
                alert.getEnvironmentId(),
                alert.getOrigin(),
                alert.getEquipmentId(),
                alert.getBatchId(),
                alert.getParameterName(),
                alert.getRecordedValue(),
                alert.getThresholdValue(),
                alert.getUnit(),
                alert.getTimestamp(),
                alert.getSeverity()
        );
    }
}