package com.iotech.qualitrack.platform.batch.domain.model.commands;

import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;

import java.math.BigDecimal;

/**
 * Command to register the consumption of a raw material lot by a product batch (US75, TS65).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment of the product
 * @param productId the product identifier
 * @param batchId the product batch that consumes the material
 * @param rawMaterialBatchId the Inventory raw material lot to consume
 * @param amountUsed the consumed amount, greater than zero
 * @param unit the unit of the amount; it must be compatible with the lot unit
 * @param operationId idempotency key chosen by the client (maximum 100 characters); retrying with the same
 *                    key returns the original usage instead of consuming twice
 */
public record RegisterRawMaterialUsageCommand(
        Long laboratoryId,
        Long environmentId,
        Long productId,
        Long batchId,
        Long rawMaterialBatchId,
        BigDecimal amountUsed,
        String unit,
        String operationId
) {
    public RegisterRawMaterialUsageCommand {
        if (batchId == null || batchId <= 0) throw new IllegalArgumentException("batchId cannot be null or less than 1");
        if (rawMaterialBatchId == null || rawMaterialBatchId <= 0) {
            throw new IllegalArgumentException("rawMaterialBatchId cannot be null or less than 1");
        }
        unit = StockUnit.normalize(unit);
        StockUnit.validateQuantity(amountUsed, unit, false);
        if (operationId == null || operationId.isBlank()) throw new IllegalArgumentException("operationId cannot be null or blank");
        operationId = operationId.trim();
        if (operationId.length() > 100) throw new IllegalArgumentException("operationId cannot exceed 100 characters");
    }
}
