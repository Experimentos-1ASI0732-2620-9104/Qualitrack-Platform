package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.DeviationAlertDetail;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.DeviationAlertDetailResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.DeviationAlertResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.RelatedActuationResource;

/**
 * Assembler to convert DeviationAlert domain models into REST resources.
 */
public class DeviationAlertResourceFromEntityAssembler {

    public static DeviationAlertResource toResourceFromEntity(DeviationAlert entity) {
        return new DeviationAlertResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getEnvironmentId(),
                entity.getOrigin(),
                entity.getEquipmentId(),
                entity.getBatchId(),
                entity.getMeasurementId(),
                entity.getLastMeasurementId(),
                entity.getParameterName(),
                entity.getRecordedValue(),
                entity.getThresholdValue(),
                entity.getUnit(),
                entity.getTimestamp(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getDeviationCount(),
                entity.getLastDetectedAt(),
                entity.getNormalizedAt(),
                entity.getAcknowledgedBy(),
                entity.getAcknowledgedAt(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getResolutionNotes()
        );
    }

    public static DeviationAlertDetailResource toDetailResourceFromEntity(DeviationAlertDetail detail) {
        var entity = detail.alert();
        return new DeviationAlertDetailResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getEnvironmentId(),
                entity.getOrigin(),
                entity.getEquipmentId(),
                entity.getBatchId(),
                entity.getMeasurementId(),
                entity.getLastMeasurementId(),
                entity.getParameterName(),
                entity.getRecordedValue(),
                entity.getThresholdValue(),
                entity.getUnit(),
                entity.getTimestamp(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getDeviationCount(),
                entity.getLastDetectedAt(),
                entity.getNormalizedAt(),
                entity.getAcknowledgedBy(),
                entity.getAcknowledgedAt(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getResolutionNotes(),
                detail.actuations().stream()
                        .map(action -> new RelatedActuationResource(action.id(), action.action(), action.triggerState(),
                                action.result(), action.occurredAt()))
                        .toList()
        );
    }
}
