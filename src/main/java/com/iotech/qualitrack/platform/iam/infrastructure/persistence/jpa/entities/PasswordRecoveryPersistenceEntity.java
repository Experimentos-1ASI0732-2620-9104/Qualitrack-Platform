package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordRecoveryStatus;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA persistence entity of a password recovery; the verification code is stored only as a hash.
 */
@Getter
@Setter
@Entity
@Table(name = "iam_password_recoveries", indexes = @Index(name = "idx_password_recovery_user", columnList = "user_id"))
public class PasswordRecoveryPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PasswordRecoveryStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;
}
