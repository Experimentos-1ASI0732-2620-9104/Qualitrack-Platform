package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials;

/**
 * Delivers the sign-in credentials of a new staff account to the staff member.
 */
public interface CredentialsNotifier {

    /**
     * Sends the credentials to the staff member.
     *
     * @return true when the message was handed to the delivery service; false when delivery is not
     * configured or failed, in which case the quality manager must hand over the temporary password
     */
    boolean send(StaffCredentials credentials);

    /**
     * Credentials of a new staff account.
     *
     * @param email address of the staff member
     * @param fullName name used in the greeting
     * @param username username of the account
     * @param temporaryPassword password to change at the first sign in
     */
    record StaffCredentials(String email, String fullName, String username, String temporaryPassword) {
    }
}
