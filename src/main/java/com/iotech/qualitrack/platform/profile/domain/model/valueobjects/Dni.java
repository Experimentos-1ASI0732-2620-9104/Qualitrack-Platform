package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Peruvian national identity document (DNI).
 *
 * @param value the 8 digits of the document
 */
public record Dni(String value) {
    private static final Pattern FORMAT = Pattern.compile("^\\d{8}$");

    public Dni {
        if (value == null) throw new IllegalArgumentException("The DNI is required");
        value = value.trim();
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("The DNI must have 8 digits");
    }
}
