package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.DeviationAlertPersistenceEntity;

/**
 * Static assembler between deviation alert domain and persistence representations.
 */
public final class DeviationAlertPersistenceAssembler {

    private DeviationAlertPersistenceAssembler() {
    }

    public static DeviationAlert toDomainFromPersistence(DeviationAlertPersistenceEntity entity) {
        if (entity == null) return null;

        return new DeviationAlert(
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

    public static DeviationAlertPersistenceEntity toPersistenceFromDomain(DeviationAlert alert) {
        if (alert == null) return null;

        var entity = new DeviationAlertPersistenceEntity();

        if (alert.getId() != null) {
            entity.setId(alert.getId());
        }

        entity.setLaboratoryId(alert.getLaboratoryId());
        entity.setEnvironmentId(alert.getEnvironmentId());
        entity.setOrigin(alert.getOrigin());
        entity.setEquipmentId(alert.getEquipmentId());
        entity.setMeasurementId(alert.getMeasurementId());
        entity.setLastMeasurementId(alert.getLastMeasurementId());
        entity.setDeviationCount(alert.getDeviationCount());
        entity.setLastDetectedAt(alert.getLastDetectedAt());
        entity.setNormalizedAt(alert.getNormalizedAt());
        entity.setAcknowledgedAt(alert.getAcknowledgedAt());
        entity.setResolvedAt(alert.getResolvedAt());
        entity.setBatchId(alert.getBatchId());
        entity.setParameterName(alert.getParameterName());
        entity.setRecordedValue(alert.getRecordedValue());
        entity.setThresholdValue(alert.getThresholdValue());
        entity.setUnit(alert.getUnit());
        entity.setTimestamp(alert.getTimestamp());
        entity.setSeverity(alert.getSeverity());
        entity.setStatus(alert.getStatus());
        entity.setAcknowledgedBy(alert.getAcknowledgedBy());
        entity.setResolvedBy(alert.getResolvedBy());
        entity.setResolutionNotes(alert.getResolutionNotes());

        return entity;
    }
}