package com.iotech.qualitrack.platform.batch.domain.model.entities;

import lombok.Getter;

/**
 * The RawMaterialUsage domain entity.
 *
 * <p>Represents the consumption of a raw material lot within a production batch. It is created from the
 * consumption confirmed by Inventory Management, so it keeps the exact lot, quantity and stock change.</p>
 */
@Getter
public class RawMaterialUsage {

    /**
     * The unique internal numeric identifier for the usage record.
     */
    private Long id;
    private Long inventoryReceiptId;

    public void assignInventoryReceipt(Long receiptId) { this.inventoryReceiptId = receiptId; }

    /**
     * Idempotency key of the Inventory consumption that produced this usage.
     */
    private String operationId;

    public void assignOperation(String operationId) { this.operationId = operationId; }
    private java.math.BigDecimal stockBefore;
    private java.math.BigDecimal stockAfter;

    public void recordStockChange(java.math.BigDecimal before, java.math.BigDecimal after) {
        this.stockBefore = before;
        this.stockAfter = after;
    }

    /**
     * The numeric identifier of the production batch.
     */
    private Long batchId;

    /**
     * The numeric identifier of the consumed raw material.
     */
    private Long rawMaterialId;

    /**
     * The display name of the consumed raw material.
     */
    private String rawMaterialName;

    /**
     * The quantity of raw material consumed.
     */
    private Double quantityUsed;

    /**
     * The unit of measurement for the consumed quantity.
     */
    private String unit;

    /**
     * The date when the raw material was used.
     */
    private String usageDate;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public RawMaterialUsage() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a RawMaterialUsage entity from persistence data.
     *
     * @param id The unique numeric ID.
     * @param batchId The batch ID.
     * @param rawMaterialId The raw material ID.
     * @param rawMaterialName The raw material display name.
     * @param quantityUsed The consumed quantity.
     * @param unit The measurement unit.
     * @param usageDate The usage date.
     */
    public RawMaterialUsage(Long id, Long batchId, Long rawMaterialId, String rawMaterialName,
                            Double quantityUsed, String unit, String usageDate) {
        this.id = id;
        this.batchId = batchId;
        this.rawMaterialId = rawMaterialId;
        this.rawMaterialName = rawMaterialName;
        this.quantityUsed = quantityUsed;
        this.unit = unit;
        this.usageDate = usageDate;
    }

}
