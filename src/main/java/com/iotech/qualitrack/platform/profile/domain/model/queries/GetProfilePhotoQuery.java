package com.iotech.qualitrack.platform.profile.domain.model.queries;

/**
 * Query for the photo of the profile of an account.
 *
 * @param userId the account
 */
public record GetProfilePhotoQuery(Long userId) {
    public GetProfilePhotoQuery {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
    }
}
