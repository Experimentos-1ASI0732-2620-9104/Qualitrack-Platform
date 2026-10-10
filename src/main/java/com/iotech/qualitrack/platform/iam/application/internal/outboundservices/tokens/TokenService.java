package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.tokens;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;

/**
 * Outbound service contract for token generation and validation.
 */
public interface TokenService {

    /**
     * Generates an authentication token for a user.
     *
     * @param user authenticated user
     * @return generated token
     */
    String generateToken(User user);

    /**
     * Extracts the username from a token.
     *
     * @param token bearer token
     * @return username
     */
    String getUsernameFromToken(String token);

    /**
     * @return the account the token was issued to, or null when the token does not carry it
     */
    Long getUserIdFromToken(String token);

    /**
     * Validates whether a token is structurally valid and not expired.
     *
     * @param token bearer token
     * @return true when valid
     */
    boolean validateToken(String token);
}