package com.iotech.qualitrack.platform.iam.application.commandservices;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.RequestPasswordRecoveryCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

import java.time.Duration;

/**
 * Recovery of the password of an account with a verification code sent to its e-mail (US16, US17).
 */
public interface PasswordRecoveryCommandService {

    /**
     * Sends a new verification code to the e-mail of the account. The answer is the same whether or not the account
     * exists, has an e-mail or is active, so it does not reveal which accounts exist.
     *
     * @return how long the code can be used
     */
    Result<Duration, ApplicationError> handle(RequestPasswordRecoveryCommand command);

    /**
     * Sets the new password when the code matches the pending recovery of the account.
     *
     * @return the account; a validation error when the code is wrong, used, revoked or expired
     */
    Result<User, ApplicationError> handle(ResetPasswordCommand command);
}
