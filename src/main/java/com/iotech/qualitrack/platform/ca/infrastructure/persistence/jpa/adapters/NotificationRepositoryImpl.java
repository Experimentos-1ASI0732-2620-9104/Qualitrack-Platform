package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationRepository;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers.NotificationPersistenceAssembler;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.repositories.NotificationPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationPersistenceRepository notifications;

    public NotificationRepositoryImpl(NotificationPersistenceRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notifications.findById(id).map(NotificationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Notification> findByRecipient(Long recipientUserId, boolean unreadOnly, int limit) {
        var page = PageRequest.of(0, limit);
        var found = unreadOnly
                ? notifications.findByRecipientUserIdAndReadAtIsNullOrderByOccurredAtDescIdDesc(recipientUserId, page)
                : notifications.findByRecipientUserIdOrderByOccurredAtDescIdDesc(recipientUserId, page);
        return found.stream().map(NotificationPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public long countUnread(Long recipientUserId) {
        return notifications.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }

    @Override
    public Notification save(Notification notification) {
        var entity = notification.getId() == null ? new NotificationPersistenceEntity()
                : notifications.findById(notification.getId()).orElseGet(NotificationPersistenceEntity::new);
        var saved = notifications.save(NotificationPersistenceAssembler.toPersistenceFromDomain(notification, entity));
        notification.setId(saved.getId());
        return notification;
    }

    @Override
    public List<Notification> saveAll(List<Notification> toSave) {
        var entities = toSave.stream()
                .map(notification -> NotificationPersistenceAssembler.toPersistenceFromDomain(notification,
                        new NotificationPersistenceEntity()))
                .toList();
        return notifications.saveAll(entities).stream()
                .map(NotificationPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    @Transactional
    public int markAllAsRead(Long recipientUserId, Instant readAt) {
        return notifications.markAllAsRead(recipientUserId, readAt);
    }
}
