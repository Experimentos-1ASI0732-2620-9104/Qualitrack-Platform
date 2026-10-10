package com.iotech.qualitrack.platform.iam.domain.model.commands;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EmailAddress;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Username;

/**
 * Command to replace the username and the e-mail of the authenticated user, confirmed with the current password.
 *
 * @param userId authenticated user
 * @param username new username (3 to 80 characters)
 * @param email new e-mail address
 * @param currentPassword password used to sign in
 */
public record UpdateAccountCommand(Long userId, Username username, EmailAddress email, String currentPassword) {
    public UpdateAccountCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (username == null) throw new IllegalArgumentException("username is required");
        if (email == null) throw new IllegalArgumentException("email is required");
        if (currentPassword == null || currentPassword.isBlank()) throw new IllegalArgumentException("currentPassword is required");
    }

    /**
     * Builds the command from raw values, trimming the username.
     *
     * @throws IllegalArgumentException when the username or the e-mail are not valid
     */
    public static UpdateAccountCommand of(Long userId, String username, String email, String currentPassword) {
        if (username == null) throw new IllegalArgumentException("username is required");
        return new UpdateAccountCommand(userId, new Username(username.trim()), new EmailAddress(email), currentPassword);
    }
}
