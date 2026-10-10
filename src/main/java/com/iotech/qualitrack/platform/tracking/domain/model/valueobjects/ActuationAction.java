package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.util.Locale;
import java.util.Optional;

/**
 * Physical action executed by a container monitor: ventilation, simulated refrigeration or the mechanism moved by the
 * servo. The activations can be configured in an actuation rule; the device also reports when it stops them once the
 * condition goes back to normal.
 */
public enum ActuationAction {
    VENTILATION_ON(true),
    VENTILATION_OFF(false),
    COOLING_ON(true),
    COOLING_OFF(false),
    SERVO_OPEN(true),
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

    public static Optional<ActuationAction> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
