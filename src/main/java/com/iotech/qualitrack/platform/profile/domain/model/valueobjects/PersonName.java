package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import java.util.Optional;

/**
 * Full name of a person as shown to the rest of the laboratory.
 *
 * @param value 2 to 120 characters, trimmed
 */
public record PersonName(String value) {
    public static final int MAX_LENGTH = 120;

    public PersonName {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("The full name is required");
        value = value.trim().replaceAll("\\s+", " ");
        if (value.length() < 2) throw new IllegalArgumentException("The full name must have at least 2 characters");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("The full name cannot exceed " + MAX_LENGTH + " characters");
        }
    }

    /**
     * @param value a name recorded elsewhere, for example by the quality manager
     * @return the name, or empty when it is missing or not valid
     */
    public static Optional<PersonName> tryParse(String value) {
        try {
            return Optional.of(new PersonName(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
