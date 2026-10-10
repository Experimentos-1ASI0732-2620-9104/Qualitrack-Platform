package com.iotech.qualitrack.platform.iam.domain.model.commands;

/**
 * Command to send a verification code to the e-mail of an account (US16, TS05).
 *
 * @param account username or e-mail of the account
 */
public record RequestPasswordRecoveryCommand(String account) {
    public RequestPasswordRecoveryCommand {
        if (account == null || account.isBlank()) throw new IllegalArgumentException("The username or e-mail is required");
        account = account.trim();
    }
}
