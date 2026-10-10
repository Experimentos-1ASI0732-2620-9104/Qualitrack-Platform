package com.iotech.qualitrack.platform.ca.application.queryservices;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetNotificationsQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetUnreadNotificationCountQuery;

import java.util.List;

/**
 * Reads the notifications of a person.
 */
public interface NotificationQueryService {

    List<Notification> handle(GetNotificationsQuery query);

    long handle(GetUnreadNotificationCountQuery query);
}
