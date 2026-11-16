package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the {@link ReportType} enum between its domain representation
 * and the persistence column value (String).
 */
@Converter(autoApply = true)
public class ReportTypePersistenceConverter implements AttributeConverter<ReportType, String> {

    /**
     * Domain -> column. Returns {@code null} when the domain value is {@code null},
     * preserving database nullability semantics.
     */
    @Override
    public String convertToDatabaseColumn(ReportType attribute) {
        return attribute == null ? null : attribute.name();
    }

    /**
     * Column -> domain. Returns {@code null} when the stored value is {@code null}.
     *
     * <p>If the column contains a value that no longer matches any constant
     * (e.g. after an unmigrated rename), {@link ReportType#valueOf(String)}
     * throws {@link IllegalArgumentException}. This is intentional: silently
     * falling back to a default would hide data corruption.
     */
    @Override
    public ReportType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReportType.valueOf(dbData);
    }
}