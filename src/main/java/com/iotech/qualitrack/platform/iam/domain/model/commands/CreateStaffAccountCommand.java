package com.iotech.qualitrack.platform.iam.domain.model.commands;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;

import java.util.Set;

/**
 * Command to create the sign-in account of a staff member registered by a quality manager.
 *
 * @param laboratoryId laboratory the staff member works for
 * @param email staff member email, used as username and to deliver the credentials
 * @param fullName staff member name, used in the credentials message
 * @param role access role of the account: {@link Roles#ROLE_LAB_OPERATOR} or {@link Roles#ROLE_AUDITOR}
 */
public record CreateStaffAccountCommand(Long laboratoryId, String email, String fullName, Roles role) {
    private static final Set<Roles> STAFF_ROLES = Set.of(Roles.ROLE_LAB_OPERATOR, Roles.ROLE_AUDITOR);

    public CreateStaffAccountCommand {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email cannot be null or blank");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("fullName cannot be null or blank");
        if (!STAFF_ROLES.contains(role)) throw new IllegalArgumentException("Staff accounts are operators or auditors");
        email = email.trim().toLowerCase(java.util.Locale.ROOT);
        fullName = fullName.trim();
    }
}
