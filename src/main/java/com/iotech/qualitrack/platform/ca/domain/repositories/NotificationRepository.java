package com.iotech.qualitrack.platform.ca.domain.repositories;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Notification repository port.
 */
public interface NotificationRepository {

    Optional<Notification> findById(Long id);

    /**
     * @param recipientUserId the account that receives them
     * @param unreadOnly true to leave out the notifications already read
     * @param limit maximum number of notifications
     * @return the newest notifications first
     */
    List<Notification> findByRecipient(Long recipientUserId, boolean unreadOnly, int limit);

    long countUnread(Long recipientUserId);

    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    /**
     * @return how many unread notifications were marked as read
     */
    int markAllAsRead(Long recipientUserId, Instant readAt);
}
