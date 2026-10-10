package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_materials", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"laboratory_id", "code"}),
    @UniqueConstraint(columnNames = {"laboratory_id", "legacy_id"})
})
@Getter @Setter
public class InventoryMaterialEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "laboratory_id", nullable = false) private Long laboratoryId;
    /** Nullable only for materials registered before environments existed. */
    @Column(name = "environment_id") private Long environmentId;
    @Column(name = "legacy_id") private Long legacyId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 20) private String unit;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal minimumStock;
}
