package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.converters;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link AuditAction}.
 * With {@code autoApply = true} there is no need to annotate every enum field on entities.
 * The enum {@code name()} is persisted as a String and rebuilt on read.
 * If constants are ever renamed, this is the only place to migrate.
 */
@Converter(autoApply = true)
public class AuditActionPersistenceConverter implements AttributeConverter<AuditAction, String> {

    /** Domain -> column: null safe. */
    @Override
    public String convertToDatabaseColumn(AuditAction attribute) {
        return attribute == null ? null : attribute.name();
    }

    /**
     * Column -> domain: null safe.
     * Throws {@link IllegalArgumentException} if the value does not match any constant
     * (symptom of corrupted data or an unmigrated rename).
     */
    @Override
    public AuditAction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AuditAction.valueOf(dbData);
    }
}