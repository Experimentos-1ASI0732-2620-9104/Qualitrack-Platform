package com.iotech.qualitrack.platform.equipment.domain.model.commands;

/**
 * Text rules shared by the Equipment commands: values are trimmed and limited to the stored column length.
 */
final class CommandText {
    private CommandText() {
    }

    static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be null or blank");
        return limited(value.trim(), field, maximum);
    }

    static String optional(String value, String field, int maximum) {
        return value == null || value.isBlank() ? null : limited(value.trim(), field, maximum);
    }

    static Long identifier(Long value, String field) {
        if (value == null || value <= 0) throw new IllegalArgumentException(field + " cannot be null or less than 1");
        return value;
    }

    private static String limited(String value, String field, int maximum) {
        if (value.length() > maximum) throw new IllegalArgumentException(field + " cannot exceed " + maximum + " characters");
        return value;
    }
}
