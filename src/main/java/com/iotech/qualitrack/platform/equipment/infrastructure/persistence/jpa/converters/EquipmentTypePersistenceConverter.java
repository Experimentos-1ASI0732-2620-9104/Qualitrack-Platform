package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the {@link EquipmentType} value object as its plain text value in the database.
 */
@Converter(autoApply = true)
public class EquipmentTypePersistenceConverter implements AttributeConverter<EquipmentType, String> {

    @Override
    public String convertToDatabaseColumn(EquipmentType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EquipmentType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new EquipmentType(dbData);
    }
}