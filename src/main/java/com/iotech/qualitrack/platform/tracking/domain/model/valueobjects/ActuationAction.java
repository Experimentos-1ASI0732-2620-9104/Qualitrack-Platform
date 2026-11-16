package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.util.Locale;
import java.util.Optional;

/**
 * Physical action executed by a container monitor: ventilation, simulated refrigeration or the mechanism moved by the
 * servo. The activations can be configured in an actuation rule; the device also reports when it stops them once the
 * condition goes back to normal.
 */
public enum ActuationAction {
    /** Turns on the ventilation of the container. */
    VENTILATION_ON(true),
    /** Turns off the ventilation once the condition recovered. */
    VENTILATION_OFF(false),
    /** Starts the simulated refrigeration of the container. */
    COOLING_ON(true),
    /** Stops the simulated refrigeration. */
    COOLING_OFF(false),
    /** Opens the mechanism moved by the servo. */
    SERVO_OPEN(true),
    /** Closes the mechanism moved by the servo. */
    SERVO_CLOSE(false);

    private final boolean activation;

    ActuationAction(boolean activation) {
        this.activation = activation;
    }

    /**
     * Whether the action starts a response, the only kind of action a rule can request.
     */
    public boolean isActivation() {
        return activation;
    }

    /**
     * Reads an action name, ignoring case and surrounding spaces.
     *
     * @return the action; empty when the value is blank or unknown
     */
    public static Optional<ActuationAction> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
