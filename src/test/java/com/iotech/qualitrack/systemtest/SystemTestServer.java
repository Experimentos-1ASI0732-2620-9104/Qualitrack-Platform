package com.iotech.qualitrack.systemtest;

import com.iotech.qualitrack.platform.QualitrackPlatformApplication;
import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.domain.model.commands.SignUpCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Starts the real backend for the web system tests (Playwright) on http://localhost:8080.
 *
 * <p>It runs from the test classpath, so it uses the isolated H2 configuration of
 * src/test/resources/application.properties instead of MySQL. Payment is the only external
 * boundary that cannot run locally, so a quality manager with an already paid subscription is
 * seeded; every other step (sign in, laboratory, equipment, alerts) goes through the API.</p>
 *
 * <p>Run with: {@code mvn spring-boot:test-run -Dspring-boot.run.main-class=com.iotech.qualitrack.systemtest.SystemTestServer}</p>
 */
@Configuration
public class SystemTestServer {
    static final String USERNAME = "system.qa.manager";
    static final String PASSWORD = "SystemTest123!";

    public static void main(String[] args) {
        new SpringApplicationBuilder(QualitrackPlatformApplication.class, SystemTestServer.class)
                .properties("server.port=8080", "spring.datasource.url=jdbc:h2:mem:qualitrack_system;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
                .run(args);
    }

    @Bean
    ApplicationRunner seedQualityManagerWithPaidSubscription(UserCommandService users, SubscriptionRepository subscriptions) {
        return arguments -> {
            var user = users.handle(new SignUpCommand(USERNAME, PASSWORD, List.of("ROLE_QA_MANAGER"), null))
                    .toOptional().orElseThrow(() -> new IllegalStateException("System test user could not be created"));
            var end = OffsetDateTime.now().plusDays(30);
            subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(user.getId(), null,
                    PlanCode.BASIC, BillingCycle.MONTHLY, "cus_system_test", "sub_system_test", "cs_system_test",
                    end.minusMonths(1).toString(), end.toString())));
        };
    }
}
