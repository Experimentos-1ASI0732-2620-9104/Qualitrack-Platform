package com.iotech.qualitrack.platform.iam.domain.model.valueobjects;

/**
 * State of a password recovery.
 */
public enum PasswordRecoveryStatus {
    /** The code was sent and can still be used until it expires or receives too many wrong attempts. */
    PENDING,
    /** The password was reset with the code. */
    COMPLETED,
    /** A newer request replaced it, or it received too many wrong codes. */
    REVOKED
}
