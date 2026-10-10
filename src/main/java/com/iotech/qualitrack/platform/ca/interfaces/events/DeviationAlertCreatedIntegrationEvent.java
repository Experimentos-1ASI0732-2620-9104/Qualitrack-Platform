package com.iotech.qualitrack.platform.ca.interfaces.events;

import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertCreatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Integration event published by the Compliance and Alerts bounded context when a deviation alert is created.
 *
 * <p>This event is part of the published language of CA. Other bounded contexts
 * should listen to this event instead of depending on CA internal domain events.</p>
 *
 * @param alertId the deviation alert identifier
 * @param equipmentId the equipment that generated the alert
 * @param batchId the related batch identifier, if applicable
 * @param parameterName the monitored parameter that deviated
 * @param recordedValue the recorded value that triggered the alert
 * @param thresholdValue the configured threshold value
 * @param unit the measurement unit
 * @param timestamp the timestamp when the deviation occurred
 * @param severity the alert severity
 */
public record DeviationAlertCreatedIntegrationEvent(
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
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal deviation alert created domain event
     * @return the integration event
     */
    public static DeviationAlertCreatedIntegrationEvent from(DeviationAlertCreatedEvent event) {
        return new DeviationAlertCreatedIntegrationEvent(
                event.alertId(),
                event.laboratoryId(),
                event.environmentId(),
                event.origin(),
                event.equipmentId(),
                event.batchId(),
                event.parameterName(),
                event.recordedValue(),
                event.thresholdValue(),
                event.unit(),
                event.timestamp(),
                event.severity()
        );
    }
}