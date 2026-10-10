package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers 403 with the same body as the controllers when an authenticated request is denied by the URL rules
 * (subscription reserved to quality managers, read-only auditors), instead of an error dispatch that the
 * stateless chain would turn into 401.
 */
@Component
public class ForbiddenRequestHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"ACCESS_DENIED\",\"message\":\"The account cannot perform this action\"}");
    }
}
