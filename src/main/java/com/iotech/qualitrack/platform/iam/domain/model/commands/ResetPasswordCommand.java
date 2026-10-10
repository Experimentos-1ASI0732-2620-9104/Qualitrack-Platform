package com.iotech.qualitrack.platform.iam.domain.model.commands;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordPolicy;

/**
 * Command to set a new password with the verification code of a pending recovery (US17, TS06).
 *
 * @param account username or e-mail of the account
 * @param code verification code received by e-mail
 * @param newPassword new password: 8 to 72 characters with letters and digits
 */
public record ResetPasswordCommand(String account, String code, String newPassword) {
    public ResetPasswordCommand {
        if (account == null || account.isBlank()) throw new IllegalArgumentException("The username or e-mail is required");
        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("The verification code has 6 digits");
        }
        PasswordPolicy.validate(newPassword);
        account = account.trim();
        code = code.trim();
    }
}
