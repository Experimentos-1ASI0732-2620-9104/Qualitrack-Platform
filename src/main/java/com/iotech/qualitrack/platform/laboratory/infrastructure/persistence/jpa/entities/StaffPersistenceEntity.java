package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity representing a Staff Member table.
 *
 * <p>This class bridges the relational database schema with the application's
 * data access layer. It reflects the structure defined by the Angular frontend,
 * utilizing a single full name and a unique email address.</p>
 */
@Entity
@Table(name = "staff_members")
@Getter
@Setter
@NoArgsConstructor
public class StaffPersistenceEntity extends AuditableAbstractPersistenceEntity {

    // The numeric ID (Long) is inherited from AuditableAbstractPersistenceEntity.
    // The previous String 'domainId' has been removed to unify IDs across layers.

    @Column(name = "laboratory_id", nullable = false)
    private Long laboratoryId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String role;

    /**
     * Institutional or corporate email. Must be unique as it may be used
     * for system authentication.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role", length = 20)
    private StaffAccessRole accessRole;

    @Column(name = "user_id", unique = true)
    private Long userId;
}