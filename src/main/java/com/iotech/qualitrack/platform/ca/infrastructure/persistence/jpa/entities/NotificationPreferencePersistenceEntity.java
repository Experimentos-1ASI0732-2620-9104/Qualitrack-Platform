package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity representing notification preferences.
 *
 * <p>This entity stores user-specific configuration for compliance alert
 * delivery channels and severity thresholds.</p>
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreferencePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled;

    /**
     * Converted automatically by AlertSeverityPersistenceConverter.
     */
    @Column(name = "minimum_severity", nullable = false, length = 50)
    private AlertSeverity minimumSeverity;
}