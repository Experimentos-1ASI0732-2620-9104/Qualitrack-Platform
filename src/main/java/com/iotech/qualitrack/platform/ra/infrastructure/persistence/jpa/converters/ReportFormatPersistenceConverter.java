package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the {@link ReportFormat} enum between its domain representation
 * and the persistence column value (String).
 */
@Converter(autoApply = true)
public class ReportFormatPersistenceConverter implements AttributeConverter<ReportFormat, String> {

    /**
     * Domain -> column. Returns {@code null} when the domain value is {@code null},
     * preserving database nullability semantics.
     */
    @Override
    public String convertToDatabaseColumn(ReportFormat attribute) {
        return attribute == null ? null : attribute.name();
    }

    /**
     * Column -> domain. Returns {@code null} when the stored value is {@code null}.
     *
     * <p>If the column contains a value that no longer matches any constant
     * (e.g. after an unmigrated rename), {@link ReportFormat#valueOf(String)}
     * throws {@link IllegalArgumentException}. This is intentional: silently
     * falling back to a default would hide data corruption.
     */
    @Override
    public ReportFormat convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReportFormat.valueOf(dbData);
    }
}