package com.iotech.qualitrack.platform.laboratory.application.commandservices;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.DeactivateStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterStaffCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service contract for the staff of a laboratory.
 */
public interface StaffCommandService {

    /**
     * Registers a staff member and creates the account with which they sign in (US34, TS19).
     *
     * @return the staff member and its account, NOT_FOUND for an unknown laboratory, or CONFLICT when the e-mail
     * is already registered
     */
    Result<RegisteredStaff, ApplicationError> handle(RegisterStaffCommand command);

    /**
     * Deactivates a staff member of the laboratory and disables their account.
     *
     * @return the deactivated staff member, NOT_FOUND when it is not registered in the laboratory, or CONFLICT when
     * it is already inactive
     */
    Result<StaffMember, ApplicationError> handle(DeactivateStaffCommand command);

    /**
     * Account created for a staff member.
     *
     * @param userId the account identifier
     * @param username the username, the staff member e-mail
     * @param temporaryPassword the password to change at the first sign in
     * @param credentialsSent whether the credentials were e-mailed to the staff member
     */
    record StaffAccount(Long userId, String username, String temporaryPassword, boolean credentialsSent) {
    }

    /**
     * Result of registering a staff member.
     */
    record RegisteredStaff(StaffMember staffMember, StaffAccount account) {
    }
}
