package com.iotech.qualitrack.platform.ca.domain.model.commands;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;

/**
 * Command to notify the people of a laboratory, as their notification preferences allow, of something that happened.
 *
 * @param laboratoryId the laboratory
 * @param content what happened
 * @param actorUserId the person who did it, who is not notified; null when the system did it
 */
public record PublishNotificationCommand(Long laboratoryId, NotificationContent content, Long actorUserId) {
    public PublishNotificationCommand {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (content == null) throw new IllegalArgumentException("The content is required");
    }
}
