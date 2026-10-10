package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import java.time.Instant;

/**
 * Description of the photo of a profile. The image itself is kept by the photo storage.
 *
 * @param contentType media type of the image
 * @param sizeBytes size of the image
 * @param updatedAt when the photo was uploaded
 */
public record ProfilePhoto(String contentType, int sizeBytes, Instant updatedAt) {
    public ProfilePhoto {
        if (contentType == null || contentType.isBlank()) throw new IllegalArgumentException("The photo type is required");
        if (sizeBytes <= 0) throw new IllegalArgumentException("The photo size must be positive");
        if (updatedAt == null) throw new IllegalArgumentException("The photo date is required");
    }

    public static ProfilePhoto of(PhotoImage image, Instant uploadedAt) {
        return new ProfilePhoto(image.contentType(), image.size(), uploadedAt);
    }
}
