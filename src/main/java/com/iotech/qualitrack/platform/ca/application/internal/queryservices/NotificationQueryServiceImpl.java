package com.iotech.qualitrack.platform.ca.application.internal.queryservices;

import com.iotech.qualitrack.platform.ca.application.queryservices.NotificationQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetNotificationsQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetUnreadNotificationCountQuery;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> handle(GetNotificationsQuery query) {
        return notificationRepository.findByRecipient(query.userId(), query.unreadOnly(), query.limit());
    }

    @Override
    public long handle(GetUnreadNotificationCountQuery query) {
        return notificationRepository.countUnread(query.userId());
    }
}
