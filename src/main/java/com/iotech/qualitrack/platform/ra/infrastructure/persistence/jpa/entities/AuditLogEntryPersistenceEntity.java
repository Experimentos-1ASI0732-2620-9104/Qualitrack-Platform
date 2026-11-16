package com.iotech.qualitrack.platform.ra.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity representing an immutable audit log entry.
 */
@Entity
@Table(name = "audit_log_entries")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntryPersistenceEntity extends AuditableAbstractPersistenceEntity {

    /**
     * Converted automatically by AuditActionPersistenceConverter.
     */
    @Column(nullable = false, length = 50)
    private AuditAction action;

    /** Discriminator of the affected resource kind ("EQUIPMENT", "BATCH", ...). */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /** Identifier of the affected resource within {@link #entityType}; null for platform-level actions. */
    @Column(name = "entity_id")
    private Long entityId;

    /** User who performed the action; {@code null} means "system". */
    @Column(name = "performed_by")
    private Long performedBy;

    /**
     * Instant of the action as ISO-8601 text. Stored as String for portability
     * and so that lexicographic {@code BETWEEN} equals temporal {@code BETWEEN}.
     */
    @Column(nullable = false, length = 30)
    private String timestamp;

    /** Human-readable contextual payload (previous state, notes, etc.); max 1000 chars. */
    @Column(length = 1000)
    private String details;
}
