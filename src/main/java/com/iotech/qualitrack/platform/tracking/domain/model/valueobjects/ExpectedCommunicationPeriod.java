package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.time.Duration;

/**
 * Maximum time between two communications of an IoT device before it requires review (US55).
 */
public record ExpectedCommunicationPeriod(Duration value) {
    public ExpectedCommunicationPeriod {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Expected communication period must be positive and at most one day");
        }
    }
}
