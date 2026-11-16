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

    /**
     * @throws IllegalArgumentException when the value is blank or not a metric
     */
    static MonitoredMetric metric(String value) {
        return MonitoredMetric.parse(value).orElseThrow(() ->
                new IllegalArgumentException("Unknown metric: " + value + ". Use one of " + Arrays.toString(MonitoredMetric.values())));
    }

    /**
     * @return the metric, or null when the value is blank
     */
    static MonitoredMetric optionalMetric(String value) {
        return value == null || value.isBlank() ? null : metric(value);
    }

    /**
     * @throws IllegalArgumentException when the value is blank or not a state
     */
    static EnvironmentalState state(String value) {
        return enumValue(EnvironmentalState.class, value, "state");
    }

    /**
     * @return the state, or null when the value is blank
     */
    static EnvironmentalState optionalState(String value) {
        return value == null || value.isBlank() ? null : state(value);
    }

    /**
     * @throws IllegalArgumentException when the value is blank or not an action
     */
    static ActuationAction action(String value) {
        return ActuationAction.parse(value).orElseThrow(() ->
                new IllegalArgumentException("Unknown action: " + value + ". Use one of " + Arrays.toString(ActuationAction.values())));
    }

    /**
     * @return the result, or null when the value is blank
     * @throws IllegalArgumentException when the value is not a result
     */
    static ActuationResult optionalResult(String value) {
        return value == null || value.isBlank() ? null : enumValue(ActuationResult.class, value, "result");
    }

    /**
     * Parses an ISO-8601 moment with offset, such as 2026-10-03T15:00:00Z or 2026-10-03T10:00:00-05:00.
     *
     * @param field name of the request field, used in the error message
     * @throws IllegalArgumentException when the value is blank or has no offset
     */
    static Instant instant(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 date and time with offset, for example 2026-10-03T15:00:00Z");
        }
    }

    /**
     * Reads an enum constant ignoring case, listing the accepted values when it is unknown.
     */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unknown " + field + ": " + value + ". Use one of "
                    + Arrays.toString(type.getEnumConstants()));
        }
    }
}
