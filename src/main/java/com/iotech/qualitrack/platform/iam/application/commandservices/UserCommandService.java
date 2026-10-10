package com.iotech.qualitrack.platform.iam.application.commandservices;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.AssignRoleCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ChangePasswordCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.CreateStaffAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.SignInCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.SignUpCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.UpdateAccountCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application command service contract for IAM user operations.
 */
public interface UserCommandService {

    Result<AuthenticatedUser, ApplicationError> handle(SignInCommand command);

    Result<User, ApplicationError> handle(SignUpCommand command);

    Result<Long, ApplicationError> handle(AssignRoleCommand command);

    Result<Long, ApplicationError> handle(DeactivateUserCommand command);

    /**
     * Creates the account of a staff member with a temporary password and sends the credentials.
     *
     * @return the account, its temporary password and whether the credentials were e-mailed, or CONFLICT
     * when the e-mail is already the username of another account
     */
    Result<StaffAccount, ApplicationError> handle(CreateStaffAccountCommand command);

    /**
     * Replaces the password of the authenticated user and clears the temporary password requirement.
     *
     * @return the user, or VALIDATION_ERROR when the current password is not correct
     */
    Result<User, ApplicationError> handle(ChangePasswordCommand command);

    /**
     * Replaces the username and the e-mail of the account. The token of the session is issued again because it
     * identifies the user by username.
     */
    Result<AuthenticatedUser, ApplicationError> handle(UpdateAccountCommand command);

    /**
     * Account created for a staff member.
     *
     * @param user the new account
     * @param temporaryPassword the generated password, to hand over when it was not e-mailed
     * @param credentialsSent whether the credentials were e-mailed to the staff member
     */
    record StaffAccount(User user, String temporaryPassword, boolean credentialsSent) {
    }

    /**
     * Authenticated user result.
     *
     * @param user authenticated user
     * @param token generated JWT token
     */
    record AuthenticatedUser(
            User user,
            String token
    ) {
    }
}