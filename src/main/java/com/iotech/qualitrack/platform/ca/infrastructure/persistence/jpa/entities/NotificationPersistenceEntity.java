package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationType;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_recipient", columnList = "recipient_user_id, occurred_at"))
public class NotificationPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "laboratory_id", nullable = false)
    private Long laboratoryId;

    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "severity", length = 20)
    private AlertSeverity severity;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "environment_name", length = 120)
    private String environmentName;

    @Column(name = "subject_name", length = 150)
    private String subjectName;

    @Column(name = "parameter_name", length = 60)
    private String parameterName;

    @Column(name = "recorded_value")
    private Double recordedValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "read_at")
    private Instant readAt;
}
