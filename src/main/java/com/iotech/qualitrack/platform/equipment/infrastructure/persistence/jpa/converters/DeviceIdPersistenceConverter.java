package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the {@link DeviceId} value object as its plain text value in the database.
 */
@Converter(autoApply = true)
public class DeviceIdPersistenceConverter implements AttributeConverter<DeviceId, String> {

    @Override
    public String convertToDatabaseColumn(DeviceId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public DeviceId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new DeviceId(dbData);
    }
}