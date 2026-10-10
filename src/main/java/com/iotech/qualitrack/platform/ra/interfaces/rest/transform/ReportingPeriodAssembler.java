package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Reads the period of an indicator request: ISO-8601 instants, by default the last seven days until now.
 */
public final class ReportingPeriodAssembler {
    private ReportingPeriodAssembler() {
    }

    /**
     * @throws IllegalArgumentException when a value is not an ISO-8601 date-time, from is after to or the period is
     *                                  longer than 31 days (400)
     */
    public static ReportingPeriod toIndicatorPeriod(String from, String to) {
        return ReportingPeriod.forIndicators(instant(from, "from"), instant(to, "to"), Instant.now());
    }

    private static Instant instant(String value, String name) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException exception) {
            try {
                return Instant.parse(value.trim());
            } catch (DateTimeParseException ignored) {
                throw new IllegalArgumentException(name + " must be an ISO-8601 date-time with offset");
            }
        }
    }
}
