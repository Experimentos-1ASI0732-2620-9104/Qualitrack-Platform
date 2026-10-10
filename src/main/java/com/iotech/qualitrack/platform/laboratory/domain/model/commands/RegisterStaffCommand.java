package com.iotech.qualitrack.platform.laboratory.domain.model.commands;

import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;

import java.util.Locale;

/**
 * Command to register a staff member of a laboratory, who receives an account to sign in (US34, TS19).
 *
 * @param laboratoryId laboratory the staff member works for
 * @param fullName full name, up to 150 characters
 * @param role job title, up to 100 characters
 * @param email e-mail, unique, used as username of the account; up to 150 characters
 * @param accessRole operator or read-only auditor
 */
public record RegisterStaffCommand(
        Long laboratoryId,
        String fullName,
        String role,
        String email,
        StaffAccessRole accessRole
) {
    public RegisterStaffCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        fullName = required(fullName, "fullName", 150);
        role = required(role, "role", 100);
        email = required(email, "email", 150).toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("email must be a valid e-mail address");
        }
        if (accessRole == null) {
            throw new IllegalArgumentException("accessRole is required");
        }
    }

    private static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be null or blank");
        var trimmed = value.trim();
        if (trimmed.length() > maximum) throw new IllegalArgumentException(field + " cannot exceed " + maximum + " characters");
        return trimmed;
    }
}
