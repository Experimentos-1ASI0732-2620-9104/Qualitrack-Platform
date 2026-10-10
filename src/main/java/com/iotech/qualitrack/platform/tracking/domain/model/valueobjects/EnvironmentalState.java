package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * Condition of a monitored metric evaluated against the configured thresholds.
 */
public enum EnvironmentalState {
    NORMAL,
    WARNING,
    CRITICAL;

    /**
     * Whether this state is a deviation that is more severe than the previous one, so a new deviation is reported
     * only when the condition gets worse and not for every measurement of the same condition.
     *
     * @param previous state of the previous measurement, or null when there is none
     */
    public boolean worsens(EnvironmentalState previous) {
        return this != NORMAL && ordinal() > (previous == null ? NORMAL : previous).ordinal();
    }
}
