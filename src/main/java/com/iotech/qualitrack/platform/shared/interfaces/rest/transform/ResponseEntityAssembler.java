package com.iotech.qualitrack.platform.shared.interfaces.rest.transform;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.function.Function;

/**
 * Assembler that translates application Result values into HTTP responses.
 */
@NullMarked
public final class ResponseEntityAssembler {

    private ResponseEntityAssembler() {
    }

    /**
     * Converts a Result into an HTTP response using the provided success resource assembler.
     * Failure responses are delegated to ErrorResponseAssembler.
     *
     * @param result the application result
     * @param successResourceAssembler function that maps success value to response resource
     * @param successStatus HTTP status to use for successful responses
     * @param <T> success value type
     * @param <R> success response resource type
     * @return response entity for success or failure
     */
    public static <T, R> ResponseEntity<?> toResponseEntityFromResult(
            Result<T, ApplicationError> result,
            Function<T, R> successResourceAssembler,
            HttpStatusCode successStatus
    ) {
        return switch (result) {
            case Result.Success<T, ApplicationError> success ->
                    new ResponseEntity<>(successResourceAssembler.apply(success.value()), successStatus);
            case Result.Failure<T, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    /**
     * Converts a Result into a {@code 201 Created} response whose {@code Location} header points to the
     * created resource, resolved as a child path of the current request URI.
     * Failure responses are delegated to ErrorResponseAssembler.
     *
     * @param result the application result
     * @param successResourceAssembler function that maps success value to response resource
     * @param identifier function that extracts the created resource identifier from the success value
     * @param <T> success value type
     * @param <R> success response resource type
     * @return {@code 201 Created} with Location and body, or the mapped error response
     */
    public static <T, R> ResponseEntity<?> toCreatedResponseEntityFromResult(
            Result<T, ApplicationError> result,
            Function<T, R> successResourceAssembler,
            Function<T, Object> identifier
    ) {
        return switch (result) {
            case Result.Success<T, ApplicationError> success -> {
                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(identifier.apply(success.value()))
                        .toUri();
                yield ResponseEntity.created(location).body(successResourceAssembler.apply(success.value()));
            }
            case Result.Failure<T, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    /**
     * Converts a Result into a {@code 201 Created} response whose {@code Location} header points to the
     * canonical URI of the created or affected resource, which may live outside the current request path.
     * Failure responses are delegated to ErrorResponseAssembler.
     *
     * @param result the application result
     * @param successResourceAssembler function that maps success value to response resource
     * @param location function that resolves the resource URI from the success value
     * @param <T> success value type
     * @param <R> response resource type
     * @return a 201 response with Location header, or the error response for failures
     */
    public static <T, R> ResponseEntity<?> toCreatedResponseEntityAtLocation(
            Result<T, ApplicationError> result,
            Function<T, R> successResourceAssembler,
            Function<T, URI> location
    ) {
        return switch (result) {
            case Result.Success<T, ApplicationError> success ->
                    ResponseEntity.created(location.apply(success.value())).body(successResourceAssembler.apply(success.value()));
            case Result.Failure<T, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }
}
