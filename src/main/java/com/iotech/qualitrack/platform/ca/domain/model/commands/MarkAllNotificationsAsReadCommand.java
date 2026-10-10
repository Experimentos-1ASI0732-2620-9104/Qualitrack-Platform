package com.iotech.qualitrack.platform.ca.domain.model.commands;

/**
 * Command to mark every unread notification of the authenticated user as read.
 *
 * @param userId the authenticated user
 */
public record MarkAllNotificationsAsReadCommand(Long userId) {
    public MarkAllNotificationsAsReadCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
    }
}
