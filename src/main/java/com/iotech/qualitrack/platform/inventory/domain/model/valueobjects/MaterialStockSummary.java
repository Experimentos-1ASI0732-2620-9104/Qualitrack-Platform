package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Read model of a raw material with its stock derived from receipts.
 */
public record MaterialStockSummary(Long id, Long laboratoryId, Long environmentId, String code, String name, String unit,
    BigDecimal minimumStock, BigDecimal usableStock, BigDecimal physicalStock, Long legacyId) {

    /** LOW when the usable stock is strictly below the minimum stock (US41). */
    public StockStatus stockStatus() {
        return usableStock.compareTo(minimumStock) < 0 ? StockStatus.LOW : StockStatus.SUFFICIENT;
    }
}
