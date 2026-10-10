package com.iotech.qualitrack.platform.tracking.domain.model.commands;

/**
 * Shared argument checks of the Tracking commands and queries.
 */
final class TrackingCommandArguments {
    private TrackingCommandArguments() {
    }

    static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be a positive number");
    }
}
