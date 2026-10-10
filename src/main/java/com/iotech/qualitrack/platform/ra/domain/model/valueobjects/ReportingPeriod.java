package com.iotech.qualitrack.platform.ra.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Period from which an indicator or report is calculated; every indicator states it (Word 12.4).
 *
 * @param from start of the period, inclusive
 * @param to   end of the period, inclusive
 */
public record ReportingPeriod(Instant from, Instant to) {
    /**
     * Period used by the indicators when none is requested.
     */
    public static final Duration DEFAULT_LENGTH = Duration.ofDays(7);

    /**
     * Longest period of the indicators, as for the telemetry history.
     */
    public static final Duration MAX_INDICATOR_LENGTH = Duration.ofDays(31);

    public ReportingPeriod {
        if (from == null || to == null) throw new IllegalArgumentException("The period needs a start and an end");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must be before to");
    }

    /**
     * Period of the indicators: the requested one, or the last {@link #DEFAULT_LENGTH} until now.
     *
     * @throws IllegalArgumentException when the period is longer than {@link #MAX_INDICATOR_LENGTH}
     */
    public static ReportingPeriod forIndicators(Instant from, Instant to, Instant now) {
        var end = to == null ? now : to;
        var period = new ReportingPeriod(from == null ? end.minus(DEFAULT_LENGTH) : from, end);
        if (Duration.between(period.from(), period.to()).compareTo(MAX_INDICATOR_LENGTH) > 0) {
            throw new IllegalArgumentException("The period cannot be longer than 31 days");
        }
        return period;
    }

    /**
     * Period covering whole calendar days in the business time zone, as requested by the reports.
     *
     * @param startDate first day, inclusive
     * @param endDate   last day, inclusive
     * @param zone      business time zone
     */
    public static ReportingPeriod ofDays(LocalDate startDate, LocalDate endDate, ZoneId zone) {
        if (startDate.isAfter(endDate)) throw new IllegalArgumentException("Start date must not exceed end date");
        return new ReportingPeriod(startDate.atStartOfDay(zone).toInstant(),
                endDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1));
    }

    /**
     * @param moment a moment
     * @return true when the moment falls in the period
     */
    public boolean contains(Instant moment) {
        return moment != null && !moment.isBefore(from) && !moment.isAfter(to);
    }
}
