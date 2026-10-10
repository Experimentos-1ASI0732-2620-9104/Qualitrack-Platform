package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity of the staff who took part in product batches. A staff member is associated once per batch.
 */
@Entity
@Table(name = "batch_staff_participations", uniqueConstraints = @UniqueConstraint(
        name = "uk_batch_staff_participations_batch_staff", columnNames = {"batch_id", "staff_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StaffParticipationPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", nullable = false, length = 150)
    private String staffName;

    @Column(name = "staff_role", length = 100)
    private String staffRole;

    @Column(name = "registered_by_user_id")
    private Long registeredByUserId;

    @Column(name = "registered_at", nullable = false, length = 30)
    private String registeredAt;
}
