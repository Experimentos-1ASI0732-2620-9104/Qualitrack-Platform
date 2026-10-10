package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity representing a raw material usage record.
 *
 * <p>This entity stores the traceability link between a production batch and
 * the raw material consumed during manufacturing.</p>
 */
@Entity
@Table(name = "raw_material_usages")
@Getter
@Setter
@NoArgsConstructor
public class RawMaterialUsagePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "inventory_receipt_id")
    private Long inventoryReceiptId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "stock_before", precision = 19, scale = 3)
    private java.math.BigDecimal stockBefore;

    @Column(name = "stock_after", precision = 19, scale = 3)
    private java.math.BigDecimal stockAfter;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "raw_material_name", nullable = false, length = 150)
    private String rawMaterialName;

    @Column(name = "quantity_used", nullable = false)
    private Double quantityUsed;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "usage_date", nullable = false, length = 30)
    private String usageDate;

    /**
     * Idempotency key of the Inventory consumption that produced this usage; null for legacy usages.
     */
    @Column(name = "operation_id", length = 100)
    private String operationId;
}
