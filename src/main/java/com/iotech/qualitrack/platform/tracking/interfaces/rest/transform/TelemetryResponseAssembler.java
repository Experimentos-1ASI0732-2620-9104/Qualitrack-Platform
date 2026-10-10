package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.tracking.application.commandservices.TrackingCommandService.Recorded;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Responses and periods shared by the telemetry endpoints.
 */
public final class TelemetryResponseAssembler {

    /**
     * Period returned when the request does not give one.
     */
    private static final Duration DEFAULT_PERIOD = Duration.ofHours(24);

    /**
     * Longest period of one query, so a request cannot load the whole history at once.
     */
    private static final Duration MAX_PERIOD = Duration.ofDays(31);

    private TelemetryResponseAssembler() {
    }

    /**
     * 201 Created for a new record, 200 OK for a record the Edge sent again; the error response otherwise.
     */
    public static <T, R> ResponseEntity<?> toRecordedResponse(Result<Recorded<T>, ApplicationError> result,
                                                              Function<T, R> assembler) {
        return switch (result) {
            case Result.Success<Recorded<T>, ApplicationError> success -> ResponseEntity
                    .status(success.value().created() ? HttpStatus.CREATED : HttpStatus.OK)
                    .body(assembler.apply(success.value().value()));
            case Result.Failure<Recorded<T>, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    /**
     * Resolves the period of a query: the last 24 hours by default, at most 31 days.
     *
     * @return {from, to}
     * @throws IllegalArgumentException when the dates are not valid (400)
     */
    public static Instant[] period(String from, String to) {
        var end = to == null || to.isBlank() ? Instant.now() : TrackingRequestValues.instant(to, "to");
        var start = from == null || from.isBlank() ? end.minus(DEFAULT_PERIOD) : TrackingRequestValues.instant(from, "from");
        if (start.isAfter(end)) throw new IllegalArgumentException("from must be before to");
        if (Duration.between(start, end).compareTo(MAX_PERIOD) > 0) {
            throw new IllegalArgumentException("The period cannot be longer than 31 days");
        }
        return new Instant[]{start, end};
    }

    /**
     * Metric filter of a query; null when not given.
     */
    public static MonitoredMetric metric(String value) {
        return TrackingRequestValues.optionalMetric(value);
    }
}
