package com.iotech.qualitrack.platform.laboratory.domain.model.commands;

/**
 * Command to deactivate a staff member of a laboratory; the staff member can no longer sign in.
 *
 * @param laboratoryId laboratory the staff member works for
 * @param staffMemberId staff member to deactivate
 */
public record DeactivateStaffCommand(
        Long laboratoryId,
        Long staffMemberId
) {
    public DeactivateStaffCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (staffMemberId == null || staffMemberId <= 0) {
            throw new IllegalArgumentException("staffMemberId cannot be null or less than 1");
        }
    }
}
