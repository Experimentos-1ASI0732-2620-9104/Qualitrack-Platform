package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the {@link EquipmentStatus} of an equipment as its name in the database.
 */
@Converter(autoApply = true)
public class EquipmentStatusPersistenceConverter implements AttributeConverter<EquipmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(EquipmentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EquipmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EquipmentStatus.valueOf(dbData);
    }
}