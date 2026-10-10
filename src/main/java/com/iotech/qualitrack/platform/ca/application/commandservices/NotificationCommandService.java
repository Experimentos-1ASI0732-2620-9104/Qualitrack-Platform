package com.iotech.qualitrack.platform.ca.application.commandservices;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkAllNotificationsAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkNotificationAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.PublishNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.SendAlertEmailNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertEmailDelivery;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

import java.util.List;

/**
 * Notifies the people of a laboratory of what happens to its alerts and batches (US83, US84).
 */
public interface NotificationCommandService {

    /**
     * Creates the notification of each active account of the laboratory whose preferences allow it, except the
     * person who did it.
     *
     * @return the notifications created
     */
    List<Notification> handle(PublishNotificationCommand command);

    /**
     * Marks a notification of the user as read; reading it again changes nothing.
     */
    Result<Notification, ApplicationError> handle(MarkNotificationAsReadCommand command);

    /**
     * @return how many notifications were unread
     */
    int handle(MarkAllNotificationsAsReadCommand command);

    /**
     * E-mails an open critical alert to the people of the laboratory who enabled e-mail notices (US84, TS78).
     */
    Result<AlertEmailDelivery, ApplicationError> handle(SendAlertEmailNotificationCommand command);
}
