package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the {@link EnvironmentUsage} enum between its domain representation
 * and the persistence column value (String).
 */
@Converter(autoApply = true)
public class EnvironmentUsagePersistenceConverter implements AttributeConverter<EnvironmentUsage, String> {

    @Override
    public String convertToDatabaseColumn(EnvironmentUsage attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EnvironmentUsage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EnvironmentUsage.valueOf(dbData);
    }
}
