package com.iotech.qualitrack.platform.subscription.application;

import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionPlanCommandService;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.SeedSubscriptionPlansCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPlan;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionPlanCatalog;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The plans of the catalog are created at startup when the database does not have them, so they can be selected (EP03 US19).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "subscription.plans.stripe-prices.basic-monthly=price_seed_basic_monthly")
class SubscriptionPlanSeedIntegrationTests {

    @LocalServerPort int port;
    @Autowired SubscriptionPlanRepository plans;
    @Autowired SubscriptionPlanCommandService planCommands;
    @Autowired SubscriptionPlanCatalog catalog;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void startupCreatesTheCatalogPlansOnlyOnce() throws Exception {
        assertThat(plans.findAll()).extracting(plan -> plan.getCode() + " " + plan.getBillingCycle())
                .containsExactlyInAnyOrder("BASIC MONTHLY", "BASIC YEARLY", "ENTERPRISE MONTHLY", "ENTERPRISE YEARLY");
        var basicMonthly = plans.findByCodeAndBillingCycle(PlanCode.BASIC, BillingCycle.MONTHLY).orElseThrow();
        assertThat(basicMonthly.getStripePriceId()).isEqualTo("price_seed_basic_monthly");
        assertThat(basicMonthly.getMaxEquipment()).isEqualTo(5);
        assertThat(plans.findByCodeAndBillingCycle(PlanCode.ENTERPRISE, BillingCycle.YEARLY).orElseThrow()
                .getMaxEquipment()).isNull();

        assertThat(planCommands.handle(new SeedSubscriptionPlansCommand(catalog)).toOptional()).contains(0);
        assertThat(plans.findAll()).hasSize(4);

        var listed = call("/subscription-plans", token());
        assertThat(listed.statusCode()).withFailMessage(listed.body()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(listed.body(), "$")).hasSize(4);
    }

    @Test
    void storedPlansAreNotChangedAndATakenStripePriceIsNotReused() {
        var changed = new SubscriptionPlanCatalog(List.of(
                offer(PlanCode.BASIC, BillingCycle.MONTHLY, "Changed name", null),
                offer(PlanCode.PROFESSIONAL, BillingCycle.MONTHLY, "Professional", "price_seed_basic_monthly")));

        assertThat(planCommands.handle(new SeedSubscriptionPlansCommand(changed)).toOptional()).contains(0);
        var basicMonthly = plans.findByCodeAndBillingCycle(PlanCode.BASIC, BillingCycle.MONTHLY).orElseThrow();
        assertThat(basicMonthly.getName()).isEqualTo("Standard Lab");
        assertThat(basicMonthly.getStripePriceId()).isEqualTo("price_seed_basic_monthly");
        assertThat(plans.findByCodeAndBillingCycle(PlanCode.PROFESSIONAL, BillingCycle.MONTHLY)).isEmpty();
    }

    private SubscriptionPlan offer(PlanCode code, BillingCycle cycle, String name, String stripePriceId) {
        return SubscriptionPlan.offer(code, name, "Test fixture", new Money(new BigDecimal("299.00"), "USD"),
                cycle, stripePriceId, 10, 5);
    }

    private String token() throws Exception {
        var username = "plans-" + UUID.randomUUID();
        var registration = send("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"]}
                """.formatted(username));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var signIn = send("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(signIn.statusCode()).withFailMessage(signIn.body()).isEqualTo(200);
        return JsonPath.read(signIn.body(), "$.token");
    }

    private HttpResponse<String> call(String path, String token) throws Exception {
        return send("GET", path, token, null);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
