package com.iotech.qualitrack.platform.ca.interfaces.events;

import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertEscalatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Integration event published when the severity of an open alert rises because its incident got worse.
 */
public record DeviationAlertEscalatedIntegrationEvent(
        Long alertId,
        Long laboratoryId,
        Long environmentId,
        Long equipmentId,
        String parameterName,
        Double recordedValue,
        Double thresholdValue,
        String unit,
        AlertSeverity severity,
        Integer deviationCount
) {
    public static DeviationAlertEscalatedIntegrationEvent from(DeviationAlertEscalatedEvent event) {
        return new DeviationAlertEscalatedIntegrationEvent(event.alertId(), event.laboratoryId(), event.environmentId(),
                event.equipmentId(), event.parameterName(), event.recordedValue(), event.thresholdValue(), event.unit(),
                event.severity(), event.deviationCount());
    }
}
