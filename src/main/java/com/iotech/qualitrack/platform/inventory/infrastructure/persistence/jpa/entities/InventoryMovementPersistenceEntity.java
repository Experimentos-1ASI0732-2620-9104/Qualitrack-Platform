package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements", uniqueConstraints =
    @UniqueConstraint(columnNames = {"laboratory_id", "operation_id"}))
@Getter @Setter
public class InventoryMovementPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "laboratory_id", nullable = false) private Long laboratoryId;
    @Column(nullable = false) private Long materialId;
    @Column(nullable = false) private Long receiptId;
    private Long productBatchId;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal amount;
    @Column(nullable = false, length = 20) private String unit;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal stockBefore;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal stockAfter;
    @Column(length = 20) private String statusBefore;
    @Column(nullable = false, length = 20) private String statusAfter;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false) private Long actorId;
    @Column(nullable = false) private Instant occurredAt;
    @Column(name = "operation_id", length = 100) private String operationId;
}
