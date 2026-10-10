package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Contact phone number, optionally with the international prefix.
 *
 * @param value digits, spaces, hyphens and parentheses with an optional leading +, 6 to 15 digits
 */
public record PhoneNumber(String value) {
    private static final Pattern FORMAT = Pattern.compile("^\\+?[0-9 ()-]+$");

    public PhoneNumber {
        if (value == null) throw new IllegalArgumentException("The phone number is required");
        value = value.trim().replaceAll("\\s+", " ");
        var digits = value.chars().filter(Character::isDigit).count();
        if (!FORMAT.matcher(value).matches() || digits < 6 || digits > 15) {
            throw new IllegalArgumentException("The phone number must have 6 to 15 digits and an optional + prefix");
        }
    }
}
