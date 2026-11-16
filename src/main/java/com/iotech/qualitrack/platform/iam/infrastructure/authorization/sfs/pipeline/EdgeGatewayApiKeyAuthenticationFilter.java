package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.iotech.qualitrack.platform.iam.application.internal.queryservices.EdgeGatewayAuthenticationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates Edge Gateway HTTP requests using X-API-Key.
 *
 * <p>Unlike the human authentication filter, this filter
 * does not process JWTs or load UserDetailsImpl.</p>
 *
 * <p>The Gateway external identifier is obtained from
 * the request path and validated against its API key.</p>
 */
public class EdgeGatewayApiKeyAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private static final String GATEWAY_PATH =
            "/api/v1/edge-gateways/";

    private final EdgeGatewayAuthenticationService authenticationService;

    public EdgeGatewayApiKeyAuthenticationFilter(
            EdgeGatewayAuthenticationService authenticationService) {

        this.authenticationService = authenticationService;
    }

    /**
     * Validates the Gateway credentials before forwarding
     * the HTTP request to its controller.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        if (!path.startsWith(GATEWAY_PATH)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String remainingPath = path.substring(GATEWAY_PATH.length());

        int separator = remainingPath.indexOf('/');

        if (separator <= 0) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String deviceId = remainingPath.substring(0, separator);
        String apiKey = request.getHeader(API_KEY_HEADER);

        var gateway = authenticationService.authenticate(
                deviceId,
                apiKey
        );

        if (gateway.isEmpty()) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Edge Gateway credentials"
            );
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                gateway.get(),
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_EDGE_GATEWAY")
                )
        );

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}