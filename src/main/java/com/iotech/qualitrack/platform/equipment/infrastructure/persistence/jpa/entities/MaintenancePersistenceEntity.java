package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.MaintenanceType;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * JPA persistence entity representing a Maintenance Record table.
 */
@Entity
@Table(name = "maintenance_records")
@Getter
@Setter
@NoArgsConstructor
public class MaintenancePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /**
     * Environment where the equipment was located when the maintenance was registered.
     */
    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(name = "technician_name", nullable = false, length = 150)
    private String technicianName;

    @Column(name = "technician_staff_id")
    private Long technicianStaffId;

    @Column(nullable = false, length = 1000)
    private String description;

    /**
     * Convertido automáticamente por MaintenanceTypePersistenceConverter (Enum).
     */
    @Column(nullable = false, length = 50)
    private MaintenanceType type;
}