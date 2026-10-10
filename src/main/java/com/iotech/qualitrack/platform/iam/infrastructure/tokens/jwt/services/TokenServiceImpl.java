package com.iotech.qualitrack.platform.iam.infrastructure.tokens.jwt.services;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT implementation for IAM token operations.
 */
@Service
public class TokenServiceImpl implements TokenService {

    private final String secret;
    private final Integer expirationDays;

    public TokenServiceImpl(
            @Value("${authorization.jwt.secret}") String secret,
            @Value("${authorization.jwt.expiration.days}") Integer expirationDays
    ) {
        this.secret = secret;
        this.expirationDays = expirationDays;
    }

    @Override
    public String generateToken(User user) {
        var now = Instant.now();
        var expiration = now.plusSeconds(expirationDays * 24L * 60L * 60L);

        return Jwts.builder()
                .subject(user.getUsernameValue())
                .claim("userId", user.getId())
                .claim("laboratoryId", user.getLaboratoryId())
                .claim("roles", user.getRoles().stream().map(role -> role.getName().name()).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey())
                .compact();
    }

    @Override
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public Long getUserIdFromToken(String token) {
        var userId = getClaims(token).get("userId");
        return userId instanceof Number number ? number.longValue() : null;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}