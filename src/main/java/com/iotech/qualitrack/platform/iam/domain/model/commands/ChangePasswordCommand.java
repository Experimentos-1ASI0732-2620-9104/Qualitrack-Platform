package com.iotech.qualitrack.platform.iam.domain.model.commands;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordPolicy;

/**
 * Command to replace the password of the authenticated user, for example the temporary password of a staff account.
 *
 * @param userId authenticated user
 * @param currentPassword password used to sign in
 * @param newPassword new password: 8 to 72 characters with at least one letter and one digit
 */
public record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {
    public ChangePasswordCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (currentPassword == null || currentPassword.isBlank()) throw new IllegalArgumentException("currentPassword is required");
        PasswordPolicy.validate(newPassword);
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("The new password must be different from the current one");
        }
    }
}
