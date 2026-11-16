package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditLogRepository;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers TS15-TS18 (environments) and US30-US33 through the public REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvironmentIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired IamContextFacade iam;
    @Autowired AuditLogRepository audit;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20300000000L);

    private record Account(Long id, String token) {}

    private record Laboratory(Account manager, Long id) {}

    @Test
    void qualityManagerRegistersListsUpdatesAndClassifiesEnvironments() throws Exception {
        var lab = readyLaboratory();
        var base = "/laboratories/" + lab.id() + "/environments";

        var empty = call("GET", base, lab.manager().token(), null);
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(empty.body()).isEqualTo("[]");

        var created = call("POST", base, lab.manager().token(),
                "{\"code\":\"wh-rm-01\",\"name\":\"Raw material warehouse\",\"description\":\"Main room\"}");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"code\":\"WH-RM-01\"").contains("\"usage\":null");
        Long environmentId = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/api/v1" + base + "/" + environmentId));

        var duplicate = call("POST", base, lab.manager().token(), "{\"code\":\"WH-RM-01\",\"name\":\"Other\"}");
        assertThat(duplicate.statusCode()).isEqualTo(409);

        var invalid = call("POST", base, lab.manager().token(), "{\"code\":\"\",\"name\":\"No code\"}");
        assertThat(invalid.statusCode()).isEqualTo(400);

        var usage = call("POST", base + "/" + environmentId + "/usage-assignments", lab.manager().token(),
                "{\"usage\":\"RAW_MATERIAL_STORAGE\"}");
        assertThat(usage.statusCode()).withFailMessage(usage.body()).isEqualTo(201);
        assertThat(usage.body()).contains("\"usage\":\"RAW_MATERIAL_STORAGE\"")
                .contains("\"assignedBy\":" + lab.manager().id());

        assertThat(call("POST", base + "/" + environmentId + "/usage-assignments", lab.manager().token(),
                "{\"usage\":\"RAW_MATERIAL_STORAGE\"}").statusCode()).isEqualTo(409);
        assertThat(call("POST", base + "/" + environmentId + "/usage-assignments", lab.manager().token(),
                "{\"usage\":\"KITCHEN\"}").statusCode()).isEqualTo(400);

        var updated = call("PUT", base + "/" + environmentId, lab.manager().token(),
                "{\"code\":\"WH-RM-01\",\"name\":\"Raw material warehouse A\",\"description\":null}");
        assertThat(updated.statusCode()).withFailMessage(updated.body()).isEqualTo(200);
        assertThat(updated.body()).contains("\"name\":\"Raw material warehouse A\"")
                .contains("\"usage\":\"RAW_MATERIAL_STORAGE\"");

        assertThat(call("PUT", base + "/999999", lab.manager().token(),
                "{\"code\":\"X\",\"name\":\"Missing\"}").statusCode()).isIn(403, 404);

        var listed = call("GET", base, lab.manager().token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(listed.body(), "$[*].code")).containsExactly("WH-RM-01");

        assertThat(audit.findAllByPerformedBy(lab.manager().id()).stream()
                .filter(entry -> "ENVIRONMENT".equals(entry.getEntityType()) && environmentId.equals(entry.getEntityId())))
                .hasSize(3);
    }

    @Test
    void environmentsOfAnotherLaboratoryAreNotAvailable() throws Exception {
        var first = readyLaboratory();
        var second = readyLaboratory();
        var created = call("POST", "/laboratories/" + first.id() + "/environments", first.manager().token(),
                "{\"code\":\"LAB-1\",\"name\":\"Laboratory zone\"}");
        Long environmentId = ((Number) JsonPath.read(created.body(), "$.id")).longValue();

        assertThat(call("GET", "/laboratories/" + first.id() + "/environments", second.manager().token(), null)
                .statusCode()).isEqualTo(403);
        assertThat(call("GET", "/laboratories/" + second.id() + "/environments/" + environmentId,
                second.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void laboratoryOperatorCanConsultButNotChangeEnvironments() throws Exception {
        var lab = readyLaboratory();
        var base = "/laboratories/" + lab.id() + "/environments";
        call("POST", base, lab.manager().token(), "{\"code\":\"PROD-1\",\"name\":\"Production\"}");
        var operator = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Environment operator", "OPERATOR");

        assertThat(call("GET", base, operator.token(), null).statusCode()).isEqualTo(200);
        assertThat(call("POST", base, operator.token(), "{\"code\":\"PROD-2\",\"name\":\"Production 2\"}")
                .statusCode()).isEqualTo(403);
    }

    private Laboratory readyLaboratory() throws Exception {
        var manager = account("ROLE_QA_MANAGER");
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Environment laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return new Laboratory(manager, ((Number) JsonPath.read(created.body(), "$.id")).longValue());
    }

    private Account account(String role) throws Exception {
        var username = "environment-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["%2$s"],"laboratoryId":null}
                """.formatted(username, role));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), JsonPath.read(response.body(), "$.token"));
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
