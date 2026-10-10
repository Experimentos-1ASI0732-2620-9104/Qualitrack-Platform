package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA persistence entity for the history of operational status changes of equipment.
 */
@Entity
@Table(name = "equipment_status_changes")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentStatusChangePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "previous_status", nullable = false, length = 50)
    private EquipmentStatus previousStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private EquipmentStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
