package com.iotech.qualitrack.platform.tracking.domain.model.commands;

/**
 * Argument checks shared by the Tracking commands, so every command reports the same message for an invalid
 * identifier.
 */
final class TrackingCommandArguments {
    private TrackingCommandArguments() {
    }

    /**
     * @param value identifier to check
     * @param name  name used in the error message
     * @throws IllegalArgumentException when the value is null, zero or negative
     */
    static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be a positive number");
    }
}
