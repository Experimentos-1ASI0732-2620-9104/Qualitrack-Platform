package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationResult;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Converts the text values of the Tracking requests; invalid values raise IllegalArgumentException (400).
 */
final class TrackingRequestValues {

    private TrackingRequestValues() {
    }

    static MonitoredMetric metric(String value) {
        return MonitoredMetric.parse(value).orElseThrow(() ->
                new IllegalArgumentException("Unknown metric: " + value + ". Use one of " + Arrays.toString(MonitoredMetric.values())));
    }

    static MonitoredMetric optionalMetric(String value) {
        return value == null || value.isBlank() ? null : metric(value);
    }

    static EnvironmentalState state(String value) {
        return enumValue(EnvironmentalState.class, value, "state");
    }

    static EnvironmentalState optionalState(String value) {
        return value == null || value.isBlank() ? null : state(value);
    }

    static ActuationAction action(String value) {
        return ActuationAction.parse(value).orElseThrow(() ->
                new IllegalArgumentException("Unknown action: " + value + ". Use one of " + Arrays.toString(ActuationAction.values())));
    }

    static ActuationResult optionalResult(String value) {
        return value == null || value.isBlank() ? null : enumValue(ActuationResult.class, value, "result");
    }

    /**
     * Parses an ISO-8601 moment with offset, such as 2026-10-03T15:00:00Z or 2026-10-03T10:00:00-05:00.
     */
    static Instant instant(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 date and time with offset, for example 2026-10-03T15:00:00Z");
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unknown " + field + ": " + value + ". Use one of "
                    + Arrays.toString(type.getEnumConstants()));
        }
    }
}
