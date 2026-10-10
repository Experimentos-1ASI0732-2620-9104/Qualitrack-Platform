package com.iotech.qualitrack.platform.ca.domain.model.events;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Domain event published when a new deviation of an open incident raises the severity of its alert.
 */
public record DeviationAlertEscalatedEvent(
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
    public static DeviationAlertEscalatedEvent from(DeviationAlert alert) {
        return new DeviationAlertEscalatedEvent(alert.getId(), alert.getLaboratoryId(), alert.getEnvironmentId(),
                alert.getEquipmentId(), alert.getParameterName(), alert.getRecordedValue(), alert.getThresholdValue(),
                alert.getUnit(), alert.getSeverity(), alert.getDeviationCount());
    }
}
