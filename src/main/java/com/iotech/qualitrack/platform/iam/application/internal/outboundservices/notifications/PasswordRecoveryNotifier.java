package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.notifications;

import java.time.Duration;

/**
 * Sends the verification code of a password recovery to the e-mail of the account ("Send Verification Code").
 */
public interface PasswordRecoveryNotifier {

    /**
     * @return true when the message was handed to the e-mail provider
     */
    boolean send(RecoveryCode recoveryCode);

    /**
     * Verification code of a recovery.
     *
     * @param email address of the account
     * @param username username of the account, shown in the message
     * @param code the 6-digit code
     * @param validity how long the code can be used
     */
    record RecoveryCode(String email, String username, String code, Duration validity) {
    }
}
