package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

/**
 * Number of days before expiration in which a lot is considered near expiry (US42).
 */
public record NearExpiryPeriod(int days) {
    public NearExpiryPeriod {
        if (days < 0 || days > 3650) throw new IllegalArgumentException("Near expiry period must be between 0 and 3650 days");
    }
}
