package com.iotech.qualitrack.platform.ca.domain.model.queries;

/**
 * Query for how many notifications the authenticated user has not read.
 *
 * @param userId the recipient
 */
public record GetUnreadNotificationCountQuery(Long userId) {
    public GetUnreadNotificationCountQuery {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
    }
}
