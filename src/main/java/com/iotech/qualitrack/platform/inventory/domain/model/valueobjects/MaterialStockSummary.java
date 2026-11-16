package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Represents a raw material stock summary.
 *
 * <p>The stock quantities are derived from supplier batches
 * rather than stored independently in the material aggregate.</p>
 *
 * <p>Usable stock includes quantities currently available
 * for consumption according to the batch availability rules.
 * Physical stock represents the total remaining quantity
 * across registered batches.</p>
 *
 * @param id raw material identifier
 * @param laboratoryId owning laboratory identifier
 * @param environmentId assigned environment identifier
 * @param code material code
 * @param name material name
 * @param unit measurement unit
 * @param minimumStock minimum required usable stock
 * @param usableStock currently usable quantity
 * @param physicalStock total remaining physical quantity
 * @param legacyId previous material identifier, if applicable
 */
public record MaterialStockSummary(Long id, Long laboratoryId, Long environmentId, String code, String name, String unit,
    BigDecimal minimumStock, BigDecimal usableStock, BigDecimal physicalStock, Long legacyId) {


    /**
     * Determines the stock status of the raw material.
     *
     * <p>A material has LOW stock when its usable quantity
     * is strictly lower than its configured minimum stock.
     * Otherwise, its status is SUFFICIENT.</p>
     *
     * @return calculated stock status
     */
    public StockStatus stockStatus() {
        return usableStock.compareTo(minimumStock) < 0 ? StockStatus.LOW : StockStatus.SUFFICIENT;
    }
}
