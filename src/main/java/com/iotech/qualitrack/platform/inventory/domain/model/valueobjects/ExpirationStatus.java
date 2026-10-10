package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

/**
 * Expiration classification of a raw material lot on a business date (US42).
 */
public enum ExpirationStatus {
    /** The lot expires after the near expiry period. */
    VALID,
    /** The lot expires within the near expiry period and is not expired yet. */
    NEAR_EXPIRY,
    /** The expiration date has been reached. */
    EXPIRED
}
