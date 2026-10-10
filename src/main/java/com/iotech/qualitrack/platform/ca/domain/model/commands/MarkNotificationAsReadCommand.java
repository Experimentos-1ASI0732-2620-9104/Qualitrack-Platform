package com.iotech.qualitrack.platform.ca.domain.model.commands;

/**
 * Command to mark a notification of the authenticated user as read.
 *
 * @param userId the authenticated user, who must be the recipient
 * @param notificationId the notification
 */
public record MarkNotificationAsReadCommand(Long userId, Long notificationId) {
    public MarkNotificationAsReadCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException("notificationId cannot be null or less than 1");
        }
    }
}
