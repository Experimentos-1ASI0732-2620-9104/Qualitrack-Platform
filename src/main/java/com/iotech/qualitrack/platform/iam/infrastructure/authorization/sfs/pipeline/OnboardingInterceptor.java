package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.iotech.qualitrack.platform.iam.application.queryservices.UserOnboardingQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserOnboardingQuery;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OnboardingInterceptor implements HandlerInterceptor {
    private final UserOnboardingQueryService onboarding;

    public OnboardingInterceptor(UserOnboardingQueryService onboarding) {
        this.onboarding = onboarding;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        if (request.getMethod().equals("OPTIONS") || path.startsWith("/api/v1/authentication/")
                || path.equals("/api/v1/stripe/webhooks")
                || path.equals("/api/v1/users/me/onboarding")
                || path.equals("/api/v1/users/me/password-changes")
                || path.equals("/api/v1/subscription-plans")
                || path.equals("/api/v1/subscription-checkout-sessions")
                || path.startsWith("/api/v1/subscriptions/")) return true;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl user)) return false;
        var state = onboarding.handle(new GetUserOnboardingQuery(user.getId()));
        if (state.nextStep().equals("READY")) return true;
        if (state.nextStep().equals("LABORATORY") && path.equals("/api/v1/laboratories")
                && request.getMethod().equals("POST")) return true;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"ONBOARDING_REQUIRED\",\"nextStep\":\""
                + state.nextStep() + "\",\"message\":\"Complete account setup before accessing operational resources\"}");
        return false;
    }
}
