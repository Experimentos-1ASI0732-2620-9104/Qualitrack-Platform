package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity of the equipment used in product batches. An equipment is associated once per batch.
 */
@Entity
@Table(name = "batch_equipment_usages", uniqueConstraints = @UniqueConstraint(
        name = "uk_batch_equipment_usages_batch_equipment", columnNames = {"batch_id", "equipment_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EquipmentUsagePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "equipment_name", nullable = false, length = 150)
    private String equipmentName;

    @Column(name = "registered_by_user_id")
    private Long registeredByUserId;

    @Column(name = "registered_at", nullable = false, length = 30)
    private String registeredAt;
}
