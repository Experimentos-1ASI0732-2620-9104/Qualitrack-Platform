package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Requires authentication and keeps auditors read-only: a user whose only role is ROLE_AUDITOR can consult
 * records but cannot register or change them. Auditors can still manage their own account, profile, notification
 * preferences and notifications, and generate reports, which only produce evidence from existing records.
 */
public class ReadOnlyAuditorAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> WRITING_ROLES = Set.of("ROLE_ADMIN", "ROLE_QA_MANAGER", "ROLE_LAB_OPERATOR");
    private static final List<String> AUDITOR_WRITES = List.of(
            // The account, profile, notification preferences and notifications of the auditor.
            "/api/v1/users/me/**",
            "/api/v1/laboratories/*/compliance-reports",
            "/api/v1/laboratories/*/inventory/reports",
            "/api/v1/laboratories/*/environments/*/equipments/*/log-reports",
            "/api/v1/batches/*/reports");
    private final AntPathMatcher paths = new AntPathMatcher();

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        var current = authentication.get();
        if (current == null || !current.isAuthenticated() || current instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }
        var request = context.getRequest();
        if (READ_METHODS.contains(request.getMethod())) return new AuthorizationDecision(true);
        var authorities = current.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        var readOnly = authorities.contains("ROLE_AUDITOR") && authorities.stream().noneMatch(WRITING_ROLES::contains);
        if (!readOnly) return new AuthorizationDecision(true);
        var path = request.getRequestURI().substring(request.getContextPath().length());
        return new AuthorizationDecision(AUDITOR_WRITES.stream().anyMatch(pattern -> paths.match(pattern, path)));
    }
}
