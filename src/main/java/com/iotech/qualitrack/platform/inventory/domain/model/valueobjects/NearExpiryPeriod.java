package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;


/**
 * Defines the number of days used to classify supplier
 * batches as approaching expiration.
 *
 * <p>The supported range is from 0 to 3650 days.</p>
 *
 * @param days number of days before expiration
 * @throws IllegalArgumentException when days is outside the supported range
 */
public record NearExpiryPeriod(int days) {
    public NearExpiryPeriod {
        if (days < 0 || days > 3650) throw new IllegalArgumentException("Near expiry period must be between 0 and 3650 days");
    }
}
