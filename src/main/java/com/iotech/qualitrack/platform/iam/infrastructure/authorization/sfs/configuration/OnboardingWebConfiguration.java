package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.configuration;

import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.OnboardingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OnboardingWebConfiguration implements WebMvcConfigurer {
    private final OnboardingInterceptor interceptor;
    private final com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.TenantResourceInterceptor tenant;

    public OnboardingWebConfiguration(OnboardingInterceptor interceptor,
            com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.TenantResourceInterceptor tenant) {
        this.interceptor = interceptor;
        this.tenant = tenant;
    }

    /**
     * Configures onboarding and tenant validation for human users.
     *
     * <p>Edge Gateway endpoints are excluded because they use
     * API key authentication instead of a human IAM account.</p>
     *
     * @param registry MVC interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/edge-gateways/**");

        registry.addInterceptor(tenant)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/edge-gateways/**");
    }
}
