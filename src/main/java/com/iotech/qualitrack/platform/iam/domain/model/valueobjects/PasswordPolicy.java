package com.iotech.qualitrack.platform.iam.domain.model.valueobjects;

/**
 * Rules of a password chosen by a person: when it is changed and when it is reset after a recovery.
 */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    /**
     * @param newPassword the password chosen by the person
     * @throws IllegalArgumentException when it has fewer than 8 or more than 72 characters, or lacks letters or digits
     */
    public static void validate(String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH || newPassword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("The new password must have between 8 and 72 characters");
        }
        if (newPassword.chars().noneMatch(Character::isLetter) || newPassword.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("The new password must contain letters and digits");
        }
    }
}
