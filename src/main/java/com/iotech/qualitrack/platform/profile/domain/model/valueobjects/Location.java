package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

/**
 * Place where the person lives or works from, for example "Miraflores, Lima".
 *
 * @param value 2 to 120 characters, trimmed
 */
public record Location(String value) {
    public static final int MAX_LENGTH = 120;

    public Location {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("The location is required");
        value = value.trim().replaceAll("\\s+", " ");
        if (value.length() < 2) throw new IllegalArgumentException("The location must have at least 2 characters");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("The location cannot exceed " + MAX_LENGTH + " characters");
        }
    }
}
