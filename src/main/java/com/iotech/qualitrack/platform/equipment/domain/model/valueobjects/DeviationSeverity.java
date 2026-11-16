package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * Severity of a deviation of an equipment parameter, from the lowest ({@code LOW}) to the highest
 * ({@code CRITICAL}).
 */
public enum DeviationSeverity {
    LOW,
    MINOR,
    MAJOR,
    CRITICAL
}