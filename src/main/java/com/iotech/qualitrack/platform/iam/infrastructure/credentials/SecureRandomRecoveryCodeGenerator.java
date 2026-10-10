package com.iotech.qualitrack.platform.iam.infrastructure.credentials;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.RecoveryCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Verification codes of 6 digits from a cryptographically secure source.
 */
@Component
public class SecureRandomRecoveryCodeGenerator implements RecoveryCodeGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }
}
