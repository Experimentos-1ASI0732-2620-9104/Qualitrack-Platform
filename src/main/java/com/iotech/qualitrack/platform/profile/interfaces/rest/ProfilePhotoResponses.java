package com.iotech.qualitrack.platform.profile.interfaces.rest;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

/**
 * Responses shared by the profile photo endpoints.
 */
final class ProfilePhotoResponses {

    private ProfilePhotoResponses() {
    }

    /**
     * The image with its type; browsers must revalidate it because the person can replace it at any time.
     */
    static ResponseEntity<?> toPhotoResponse(Long userId, Optional<PhotoImage> photo) {
        return photo.<ResponseEntity<?>>map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(image.content()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("ProfilePhoto", userId)));
    }

    static ResponseEntity<?> tooLarge() {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResource("CONTENT_TOO_LARGE", "The photo cannot exceed 2 MB"));
    }
}
