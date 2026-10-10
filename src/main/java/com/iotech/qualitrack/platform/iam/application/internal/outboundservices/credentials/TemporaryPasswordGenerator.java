package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials;

/**
 * Generates the temporary password of a new staff account.
 */
public interface TemporaryPasswordGenerator {

    /**
     * @return a random password with letters, digits and symbols
     */
    String generate();
}
