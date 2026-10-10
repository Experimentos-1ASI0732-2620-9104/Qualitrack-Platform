package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_receipts", uniqueConstraints =
    @UniqueConstraint(columnNames = {"laboratory_id", "material_id", "supplier", "batch_number"}))
@Getter @Setter
public class InventoryReceiptEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "laboratory_id", nullable = false) private Long laboratoryId;
    @Column(name = "material_id", nullable = false) private Long materialId;
    @Column(nullable = false, length = 150) private String supplier;
    @Column(name = "batch_number", nullable = false, length = 50) private String batchNumber;
    @Column(nullable = false, length = 20) private String unit;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal initialAmount;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal availableAmount;
    @Column(nullable = false) private LocalDate receivedOn;
    @Column(nullable = false) private LocalDate expiresOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RawMaterialBatchStatus status;
    /** Container monitor of the monitored container where the lot is stored (US43). */
    @Column(name = "container_monitor_id") private Long containerMonitorId;
    @Column(name = "container_environment_id") private Long containerEnvironmentId;
    @Column(name = "container_assigned_by") private Long containerAssignedBy;
    @Column(name = "container_assigned_at") private java.time.Instant containerAssignedAt;
}
