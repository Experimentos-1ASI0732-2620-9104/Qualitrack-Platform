package com.iotech.qualitrack.platform.iam.infrastructure.credentials;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.TemporaryPasswordGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Temporary passwords of 12 characters from a cryptographically secure source, with at least one upper case
 * letter, lower case letter, digit and symbol. Easily confused characters (0, O, 1, l, I) are excluded.
 */
@Component
public class SecureRandomTemporaryPasswordGenerator implements TemporaryPasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*?";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        var characters = new ArrayList<Character>(LENGTH);
        characters.add(pick(UPPER));
        characters.add(pick(LOWER));
        characters.add(pick(DIGITS));
        characters.add(pick(SYMBOLS));
        while (characters.size() < LENGTH) characters.add(pick(ALL));
        Collections.shuffle(characters, random);
        var password = new StringBuilder(LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
