package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA persistence entity representing the environments table.
 *
 * <p>The environment code is unique within its laboratory. The usage column is named
 * {@code environment_usage} because {@code USAGE} is a reserved word in MySQL.</p>
 */
@Entity
@Table(name = "environments",
        uniqueConstraints = @UniqueConstraint(name = "uk_environments_laboratory_code",
                columnNames = {"laboratory_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class EnvironmentPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "laboratory_id", nullable = false)
    private Long laboratoryId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Converted automatically by EnvironmentUsagePersistenceConverter.
     */
    @Column(name = "environment_usage", length = 30)
    private EnvironmentUsage usage;

    @Column(name = "usage_assigned_by")
    private Long usageAssignedBy;

    @Column(name = "usage_assigned_at")
    private Instant usageAssignedAt;
}
