package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the {@link NotificationType} enum between its domain representation
 * and the persistence column value (String).
 */
@Converter(autoApply = true)
public class NotificationTypePersistenceConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationType.valueOf(dbData);
    }
}
