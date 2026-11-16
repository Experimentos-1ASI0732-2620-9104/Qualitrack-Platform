package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * Outcome reported by the device for an action it tried to execute.
 */
public enum ActuationResult {
    /** The device executed the action. */
    EXECUTED,
    /** The device tried to execute the action but could not. */
    FAILED
}
