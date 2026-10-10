package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

/**
 * Stock classification of a raw material against its minimum stock (US41).
 */
public enum StockStatus {
    /** Usable stock is below the minimum stock. */
    LOW,
    /** Usable stock is equal to or greater than the minimum stock. */
    SUFFICIENT
}
