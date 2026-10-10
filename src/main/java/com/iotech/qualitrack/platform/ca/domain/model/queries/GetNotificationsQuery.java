package com.iotech.qualitrack.platform.ca.domain.model.queries;

/**
 * Query for the notifications of the authenticated user, newest first.
 *
 * @param userId the recipient
 * @param unreadOnly true to leave out the notifications already read
 * @param limit maximum number of notifications, from 1 to 100
 */
public record GetNotificationsQuery(Long userId, boolean unreadOnly, int limit) {
    public static final int MAX_LIMIT = 100;

    public GetNotificationsQuery {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
    }
}
