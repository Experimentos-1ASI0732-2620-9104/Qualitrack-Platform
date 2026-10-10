package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.services.UserDetailsServiceImpl;
import com.iotech.qualitrack.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Request filter that authenticates requests using JWT Bearer tokens.
 */
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private final BearerTokenService bearerTokenService;
    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public BearerAuthorizationRequestFilter(
            BearerTokenService bearerTokenService,
            TokenService tokenService,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.bearerTokenService = bearerTokenService;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!bearerTokenService.hasBearerToken(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = bearerTokenService.extractToken(authorizationHeader);

        if (!tokenService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var username = tokenService.getUsernameFromToken(token);
            var userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            // Usernames can change: a token issued to another account that used this username is not valid.
            if (userDetails.isEnabled() && userDetails.getId().equals(tokenService.getUserIdFromToken(token))) {
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationTokenBuilder.build(userDetails));
            }
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
