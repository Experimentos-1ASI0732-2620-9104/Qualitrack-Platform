package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.MeasurementPersistenceEntity;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Static assembler between measurement domain and persistence representations.
 */
public final class MeasurementPersistenceAssembler {

    private MeasurementPersistenceAssembler() {
    }

    public static Measurement toDomainFromPersistence(MeasurementPersistenceEntity entity) {
        if (entity == null) return null;
        return new Measurement(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(),
                entity.getEquipmentId(), entity.getParameterName(), entity.getValue(), entity.getTextValue(),
                entity.getUnit(), entity.getTimestamp(),
                entity.getMeasuredAt() == null ? parse(entity.getTimestamp()) : entity.getMeasuredAt(),
                entity.getState(), entity.getThresholdValue(), entity.getProfileVersion(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toInstant());
    }

    public static MeasurementPersistenceEntity toPersistenceFromDomain(Measurement measurement) {
        if (measurement == null) return null;
        var entity = new MeasurementPersistenceEntity();
        if (measurement.getId() != null) entity.setId(measurement.getId());
        entity.setLaboratoryId(measurement.getLaboratoryId());
        entity.setEnvironmentId(measurement.getEnvironmentId());
        entity.setEquipmentId(measurement.getEquipmentId());
        entity.setParameterName(measurement.getParameterName());
        entity.setValue(measurement.getValue());
        entity.setTextValue(measurement.getTextValue());
        entity.setUnit(measurement.getUnit());
        entity.setTimestamp(measurement.getTimestamp());
        entity.setMeasuredAt(measurement.getMeasuredAt());
        entity.setState(measurement.getState());
        entity.setThresholdValue(measurement.getThresholdValue());
        entity.setProfileVersion(measurement.getProfileVersion());
        return entity;
    }

    /**
     * Readings stored before the measurement time column existed only kept the text timestamp.
     */
    private static Instant parse(String timestamp) {
        if (timestamp == null) return null;
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
