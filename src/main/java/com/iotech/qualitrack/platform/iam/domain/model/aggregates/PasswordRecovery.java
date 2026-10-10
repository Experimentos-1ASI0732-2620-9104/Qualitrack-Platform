package com.iotech.qualitrack.platform.iam.domain.model.aggregates;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordRecoveryStatus;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Recovery of the password of an account (US16, US17): the verification code sent to its e-mail, kept only as a hash.
 *
 * <p>The code expires {@link #CODE_VALIDITY} after it is requested, can be used once, and is revoked after
 * {@link #MAX_FAILED_ATTEMPTS} wrong codes or when a newer recovery is requested.</p>
 */
@Getter
public class PasswordRecovery extends AbstractDomainAggregateRoot<PasswordRecovery> {
    public static final Duration CODE_VALIDITY = Duration.ofMinutes(15);
    public static final int MAX_FAILED_ATTEMPTS = 5;
    /** A new code is not sent while the previous one was requested less than this time ago. */
    public static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);

    private Long id;
    private final Long userId;
    private final String codeHash;
    private final Instant requestedAt;
    private final Instant expiresAt;
    private int failedAttempts;
    private PasswordRecoveryStatus status;
    private Instant completedAt;

    /**
     * Reconstructs a recovery from persistence data.
     */
    public PasswordRecovery(Long id, Long userId, String codeHash, Instant requestedAt, Instant expiresAt,
                            int failedAttempts, PasswordRecoveryStatus status, Instant completedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "The account is required");
        this.codeHash = Objects.requireNonNull(codeHash, "The code hash is required");
        this.requestedAt = Objects.requireNonNull(requestedAt, "The request time is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "The expiration time is required");
        this.failedAttempts = failedAttempts;
        this.status = Objects.requireNonNull(status, "The status is required");
        this.completedAt = completedAt;
    }

    /**
     * Starts the recovery of an account with the hash of the code sent to its e-mail.
     *
     * @param userId the account
     * @param codeHash hash of the verification code
     * @param now moment of the request
     * @return a pending recovery that expires {@link #CODE_VALIDITY} later
     */
    public static PasswordRecovery start(Long userId, String codeHash, Instant now) {
        return new PasswordRecovery(null, userId, codeHash, now, now.plus(CODE_VALIDITY), 0,
                PasswordRecoveryStatus.PENDING, null);
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @param now current moment
     * @return whether the code can still be used
     */
    public boolean isUsable(Instant now) {
        return status == PasswordRecoveryStatus.PENDING && now.isBefore(expiresAt) && failedAttempts < MAX_FAILED_ATTEMPTS;
    }

    /**
     * @param now current moment
     * @return whether a new code must not be sent yet, because this one was requested less than a minute ago
     */
    public boolean blocksNewCode(Instant now) {
        return isUsable(now) && now.isBefore(requestedAt.plus(RESEND_INTERVAL));
    }

    /**
     * Counts a wrong code; the recovery is revoked when it reaches {@link #MAX_FAILED_ATTEMPTS}.
     */
    public void registerFailedAttempt() {
        if (status != PasswordRecoveryStatus.PENDING) return;
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) status = PasswordRecoveryStatus.REVOKED;
    }

    /**
     * Marks the code as used after the password was reset.
     *
     * @param now moment of the reset
     * @throws IllegalStateException when the code can no longer be used
     */
    public void complete(Instant now) {
        if (!isUsable(now)) throw new IllegalStateException("The recovery code is not valid or has expired");
        status = PasswordRecoveryStatus.COMPLETED;
        completedAt = now;
    }

    /**
     * Revokes a pending recovery, for example when a newer code is requested.
     */
    public void revoke() {
        if (status == PasswordRecoveryStatus.PENDING) status = PasswordRecoveryStatus.REVOKED;
    }
}
