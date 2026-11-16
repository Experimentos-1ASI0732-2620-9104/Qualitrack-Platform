package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.time.Duration;

/**
 * Maximum time between two communications of an IoT device before it requires review (US55).
 *
 * <p>Configured with {@code tracking.devices.expected-communication-period} (five minutes by default).</p>
 *
 * @param value positive duration of at most one day
 */
public record ExpectedCommunicationPeriod(Duration value) {
    /**
     * Validates the components.
     *
     * @throws IllegalArgumentException when the duration is missing, not positive or longer than one day
     */
    public ExpectedCommunicationPeriod {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Expected communication period must be positive and at most one day");
        }
    }
}
