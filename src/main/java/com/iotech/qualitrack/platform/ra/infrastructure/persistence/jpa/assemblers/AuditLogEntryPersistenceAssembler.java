package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities.AuditLogEntryPersistenceEntity;

/**
 * Static assembler between {@link AuditLogEntry} (domain) and
 * {@link AuditLogEntryPersistenceEntity} (persistence).
 * As a stateless utility class it is not instantiable (private constructor).
 * Maps field by field, with no extra logic, to keep the repository adapter clean.
 */
public final class AuditLogEntryPersistenceAssembler {

    private AuditLogEntryPersistenceAssembler() {
    }

    /** Converts a JPA entity to domain. Returns {@code null} when the input is null. */
    public static AuditLogEntry toDomainFromPersistence(AuditLogEntryPersistenceEntity entity) {
        if (entity == null) return null;

        return new AuditLogEntry(
                entity.getId(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getPerformedBy(),
                entity.getTimestamp(),
                entity.getDetails()
        );
    }

    /**
     * Converts a domain entry to a JPA entity.
     * Only sets the id when it already exists (updates); on inserts it is left null
     * so JPA can generate it.
     */
    public static AuditLogEntryPersistenceEntity toPersistenceFromDomain(AuditLogEntry auditLogEntry) {
        if (auditLogEntry == null) return null;

        var entity = new AuditLogEntryPersistenceEntity();

        if (auditLogEntry.getId() != null) {
            entity.setId(auditLogEntry.getId());
        }

        entity.setAction(auditLogEntry.getAction());
        entity.setEntityType(auditLogEntry.getEntityType());
        entity.setEntityId(auditLogEntry.getEntityId());
        entity.setPerformedBy(auditLogEntry.getPerformedBy());
        entity.setTimestamp(auditLogEntry.getTimestamp());
        entity.setDetails(auditLogEntry.getDetails());

        return entity;
    }
}