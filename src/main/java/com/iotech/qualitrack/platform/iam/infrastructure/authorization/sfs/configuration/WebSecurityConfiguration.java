package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.configuration;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.ForbiddenRequestHandler;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.ReadOnlyAuditorAuthorizationManager;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.pipeline.UnauthorizedRequestHandlerEntryPoint;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.services.UserDetailsServiceImpl;
import com.iotech.qualitrack.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for QualiTrack IAM.
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Value("${application.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    private final UserDetailsServiceImpl userDetailsService;
    private final UnauthorizedRequestHandlerEntryPoint unauthorizedRequestHandlerEntryPoint;
    private final ForbiddenRequestHandler forbiddenRequestHandler;
    private final BearerTokenService bearerTokenService;
    private final TokenService tokenService;

    public WebSecurityConfiguration(
            UserDetailsServiceImpl userDetailsService,
            UnauthorizedRequestHandlerEntryPoint unauthorizedRequestHandlerEntryPoint,
            ForbiddenRequestHandler forbiddenRequestHandler,
            BearerTokenService bearerTokenService,
            TokenService tokenService
    ) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedRequestHandlerEntryPoint = unauthorizedRequestHandlerEntryPoint;
        this.forbiddenRequestHandler = forbiddenRequestHandler;
        this.bearerTokenService = bearerTokenService;
        this.tokenService = tokenService;
    }

    @Bean
    public AuthenticationManager authenticationManager(BCryptPasswordEncoder passwordEncoder) {
        var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public BearerAuthorizationRequestFilter bearerAuthorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(
                bearerTokenService,
                tokenService,
                userDetailsService
        );
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerAuthorizationRequestFilter bearerAuthorizationRequestFilter
    ) throws Exception {
        http
                .csrf(CsrfConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandlerEntryPoint)
                                .accessDeniedHandler(forbiddenRequestHandler)
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/v1/authentication/**",
                                "/api/v1/stripe/webhooks"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Only quality managers and administrators manage the subscription of the laboratory.
                        .requestMatchers("/api/v1/subscription-checkout-sessions/**", "/api/v1/subscriptions/**",
                                "/api/v1/laboratories/*/subscriptions/**")
                        .hasAnyAuthority("ROLE_QA_MANAGER", "ROLE_ADMIN")
                        .anyRequest().access(new ReadOnlyAuditorAuthorizationManager())
                )
                .addFilterBefore(
                        bearerAuthorizationRequestFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
