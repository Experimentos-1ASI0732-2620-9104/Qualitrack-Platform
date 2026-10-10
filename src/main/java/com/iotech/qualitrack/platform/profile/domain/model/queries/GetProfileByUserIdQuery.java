package com.iotech.qualitrack.platform.profile.domain.model.queries;

/**
 * Query for the profile of an account, with the username and e-mail of the account.
 *
 * @param userId the account
 */
public record GetProfileByUserIdQuery(Long userId) {
    public GetProfileByUserIdQuery {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
    }
}
