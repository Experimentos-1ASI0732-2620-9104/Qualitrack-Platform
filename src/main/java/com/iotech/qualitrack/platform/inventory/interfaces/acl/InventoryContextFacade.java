package com.iotech.qualitrack.platform.inventory.interfaces.acl;

import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Boundary for atomic stock consumption by Product Batch Management and for the inventory read by Reporting &amp;
 * Audit.
 * The implementation validates tenant ownership and records movements under a pessimistic lock.
 * A repeated operationId must return its original result; reuse with different input must fail.
 */
public interface InventoryContextFacade {
    /**
     * Consumes stock of a raw material lot for a product batch.
     *
     * @param request the lot, product batch, amount and idempotency key
     * @return the stock change of the lot
     * @throws com.iotech.qualitrack.platform.shared.application.result.ApplicationException NOT_FOUND when the lot is
     * not in the laboratory, CONFLICT when it cannot be used, lacks stock or the operation id was used with other values
     */
    Consumption consume(ConsumptionRequest request);

    /**
     * Checks that a raw material exists in the laboratory and is kept in the environment.
     */
    boolean isRawMaterialInEnvironment(Long laboratoryId, Long environmentId, Long rawMaterialId);

    /**
     * Finds the environment where a raw material of the laboratory is kept.
     *
     * @return the environment, or empty when the material is not in the laboratory or has no environment yet
     */
    Optional<Long> findRawMaterialEnvironment(Long laboratoryId, Long rawMaterialId);

    /**
     * Raw materials kept in an environment with their stock and lots (US97).
     *
     * @param laboratoryId the laboratory of the environment
     * @param environmentId the environment
     * @return the materials ordered by code; empty when the environment keeps none
     */
    List<InventoryMaterial> findInventory(Long laboratoryId, Long environmentId);

    /**
     * Raw material of an environment shared with other bounded contexts.
     *
     * @param stockStatus LOW when the usable stock is below the minimum stock, otherwise SUFFICIENT
     */
    record InventoryMaterial(Long id, Long environmentId, String code, String name, String unit, BigDecimal minimumStock,
                             BigDecimal usableStock, BigDecimal physicalStock, String stockStatus, List<InventoryLot> lots) {
        public InventoryMaterial {
            lots = List.copyOf(lots);
        }
    }

    /**
     * Supplier lot of a raw material shared with other bounded contexts.
     *
     * @param status QUARANTINED, RELEASED, OBSERVED or REJECTED
     * @param expirationStatus VALID, NEAR_EXPIRY or EXPIRED on the business date
     * @param containerMonitorId container monitor where the lot is stored, or null
     */
    record InventoryLot(Long id, String supplier, String batchNumber, BigDecimal initialAmount,
                        BigDecimal availableAmount, LocalDate receivedOn, LocalDate expiresOn, String status,
                        String expirationStatus, Long containerMonitorId) {
    }

    record ConsumptionRequest(Long laboratoryId, Long rawMaterialBatchId, Long productBatchId,
                              BigDecimal amountUsed, String unit, String operationId) {
        public ConsumptionRequest {
            if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("Invalid laboratory ID");
            if (rawMaterialBatchId == null || rawMaterialBatchId <= 0) throw new IllegalArgumentException("Invalid receipt ID");
            if (productBatchId == null || productBatchId <= 0) throw new IllegalArgumentException("Invalid product batch ID");
            if (operationId == null || operationId.isBlank()) throw new IllegalArgumentException("Operation ID is required");
            unit = StockUnit.normalize(unit);
            StockUnit.validateQuantity(amountUsed, unit, false);
            operationId = operationId.trim();
        }
    }

    record Consumption(Long rawMaterialBatchId, Long productBatchId, BigDecimal amountUsed,
                       String unit, BigDecimal stockBefore, BigDecimal stockAfter, String operationId) { }
}
