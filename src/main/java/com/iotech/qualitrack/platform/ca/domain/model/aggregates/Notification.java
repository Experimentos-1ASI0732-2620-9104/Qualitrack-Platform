package com.iotech.qualitrack.platform.ca.domain.model.aggregates;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Notification aggregate root: a notice that reaches one person of the laboratory in QualiTrack (the bell of the web
 * application) when something they did not do happens to an alert or a batch.
 */
@Getter
public class Notification extends AbstractDomainAggregateRoot<Notification> {

    @Setter
    private Long id;

    /**
     * Account that receives the notification.
     */
    private Long recipientUserId;

    private Long laboratoryId;

    private NotificationContent content;

    private Instant occurredAt;

    /**
     * When the recipient read it, or null while it is unread.
     */
    private Instant readAt;

    /**
     * Creates an unread notification.
     */
    public Notification(Long recipientUserId, Long laboratoryId, NotificationContent content, Instant occurredAt) {
        if (recipientUserId == null || recipientUserId <= 0) throw new IllegalArgumentException("The recipient is required");
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("The laboratory is required");
        this.recipientUserId = recipientUserId;
        this.laboratoryId = laboratoryId;
        this.content = Objects.requireNonNull(content, "The content is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "The date is required");
    }

    /**
     * Reconstructs a stored notification.
     */
    public Notification(Long id, Long recipientUserId, Long laboratoryId, NotificationContent content, Instant occurredAt,
                        Instant readAt) {
        this(recipientUserId, laboratoryId, content, occurredAt);
        this.id = id;
        this.readAt = readAt;
    }

    /**
     * Marks the notification as read; reading it again keeps the first date.
     *
     * @return true when it was unread
     */
    public boolean markAsRead(Instant at) {
        if (readAt != null) return false;
        readAt = Objects.requireNonNull(at, "The date is required");
        return true;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean belongsTo(Long userId) {
        return recipientUserId.equals(userId);
    }
}
