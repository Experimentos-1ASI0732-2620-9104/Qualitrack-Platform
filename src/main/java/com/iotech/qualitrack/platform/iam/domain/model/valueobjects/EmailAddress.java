package com.iotech.qualitrack.platform.iam.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * E-mail address of an account, where the platform sends its credentials and password recovery codes. It is stored
 * trimmed and in lower case, so it identifies the account regardless of how it is typed.
 *
 * @param value normalized address
 */
public record EmailAddress(String value) {
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public static final int MAX_LENGTH = 120;

    public EmailAddress {
        Objects.requireNonNull(value, "The e-mail is required");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("The e-mail is required");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("The e-mail cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("The e-mail is not valid");
    }

    /**
     * @param value text typed by a person, for example in the sign-in or recovery form
     * @return whether the text has the format of an e-mail address
     */
    public static boolean looksLikeEmail(String value) {
        return value != null && FORMAT.matcher(value.trim()).matches();
    }
}
