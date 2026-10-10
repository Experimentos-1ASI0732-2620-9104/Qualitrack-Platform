package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

/**
 * Represents the impact level of a deviation alert, from the lowest to the highest.
 */
public enum AlertSeverity {
    LOW,
    WARNING,
    CRITICAL;

    /**
     * @param minimum the lowest severity of interest
     * @return whether this severity reaches it
     */
    public boolean isAtLeast(AlertSeverity minimum) {
        return minimum == null || compareTo(minimum) >= 0;
    }
}
