package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * Name of the critical variable that a BPM parameter controls on an equipment, such as temperature or humidity.
 *
 * @param name variable name; required and up to 100 characters
 */
public record CriticalVariable(String name) {
    public CriticalVariable {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Critical variable name cannot be null or blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Critical variable cannot exceed 100 characters");
        }
    }
}