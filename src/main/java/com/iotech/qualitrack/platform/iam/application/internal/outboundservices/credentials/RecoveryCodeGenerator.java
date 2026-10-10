package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials;

/**
 * Generates the verification code of a password recovery.
 */
public interface RecoveryCodeGenerator {

    /**
     * @return a random code of 6 digits
     */
    String generate();
}
