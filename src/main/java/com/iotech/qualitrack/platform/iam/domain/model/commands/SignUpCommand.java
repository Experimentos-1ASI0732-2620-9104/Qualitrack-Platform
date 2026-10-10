package com.iotech.qualitrack.platform.iam.domain.model.commands;

import java.util.List;

/**
 * Command used to register a new IAM user.
 *
 * @param username account username
 * @param email account e-mail, used to recover the password
 * @param password raw account password
 * @param roles requested role names
 * @param laboratoryId associated laboratory identifier
 */
public record SignUpCommand(
        String username,
        String email,
        String password,
        List<String> roles,
        Long laboratoryId
) {
    public SignUpCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be null or blank");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be null or blank");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password cannot be null or blank");
        }

        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles cannot be null or empty");
        }

        if (laboratoryId != null && laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be less than 1");
        }
    }
}