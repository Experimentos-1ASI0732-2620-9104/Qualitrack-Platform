package com.iotech.qualitrack.platform.bdd;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * State shared by the step definitions of one scenario: the acting user and the records it created.
 */
@Component
@ScenarioScope(proxyMode = ScopedProxyMode.NO)
public class ScenarioContext {
    private static final AtomicLong ruc = new AtomicLong(20300000000L);

    private final HttpClient http = HttpClient.newHttpClient();
    private final SubscriptionRepository subscriptions;
    private final int port;

    public record Manager(Long userId, String token, Long laboratoryId, Long environmentId) {}

    Manager manager;
    Long equipmentId;
    Long productId;
    Long batchId;
    Long alertId;
    HttpResponse<String> lastResponse;
    HttpResponse<byte[]> lastDocument;
    int lastStatus;

    public ScenarioContext(SubscriptionRepository subscriptions, @Value("${local.server.port}") int port) {
        this.subscriptions = subscriptions;
        this.port = port;
    }

    /** Registers a user, activates its subscription and creates its laboratory through the API. */
    Manager newManagerWithLaboratory() throws Exception {
        var username = "bdd-" + UUID.randomUUID();
        var credentials = """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username);
        assertThat(send("POST", "/authentication/sign-up", null, """
                {"username":"%s","email":"%s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username, username)).statusCode()).isEqualTo(201);
        var session = send("POST", "/authentication/sign-in", null, credentials);
        Long userId = ((Number) JsonPath.read(session.body(), "$.id")).longValue();
        String token = JsonPath.read(session.body(), "$.token");

        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(30);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(userId, null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_bdd_" + unique, "sub_bdd_" + unique,
                "cs_bdd_" + unique, end.minusMonths(1).toString(), end.toString())));

        var laboratory = send("POST", "/laboratories", token, """
                {"name":"BDD laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(laboratory.statusCode()).withFailMessage(laboratory.body()).isEqualTo(201);
        var environment = send("POST", "/laboratories/" + id(laboratory) + "/environments", token,
                "{\"code\":\"ENV-01\",\"name\":\"BDD environment\"}");
        assertThat(environment.statusCode()).withFailMessage(environment.body()).isEqualTo(201);
        return new Manager(userId, token, id(laboratory), id(environment));
    }

    /** Base path of the environment of the acting manager. */
    String environmentPath() {
        return "/laboratories/" + manager.laboratoryId() + "/environments/" + manager.environmentId();
    }

    HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var response = http.send(request(method, path, token, body), HttpResponse.BodyHandlers.ofString());
        lastStatus = response.statusCode();
        return response;
    }

    HttpResponse<byte[]> download(String method, String path, String token, String body) throws Exception {
        var response = http.send(request(method, path, token, body), HttpResponse.BodyHandlers.ofByteArray());
        lastStatus = response.statusCode();
        return response;
    }

    static long id(HttpResponse<String> response) {
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    private HttpRequest request(String method, String path, String token, String body) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return request.build();
    }
}
