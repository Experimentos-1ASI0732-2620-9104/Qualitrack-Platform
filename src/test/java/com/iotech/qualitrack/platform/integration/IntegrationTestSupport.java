package com.iotech.qualitrack.platform.integration;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared fixtures for integration tests that exercise the REST API over HTTP
 * with the real security filter chain, application services and H2 persistence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {
    private static final AtomicLong ruc = new AtomicLong(20200000000L);

    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    private final HttpClient http = HttpClient.newHttpClient();

    protected record Account(Long id, String token) {}

    protected record Staff(long staffId, long userId, String token, String fullName) {}

    protected record ReadyAccount(Account account, long laboratoryId, long environmentId) {
        public String token() { return account.token(); }
        public Long userId() { return account.id(); }
    }

    protected Account account() throws Exception {
        var username = "it-" + UUID.randomUUID();
        var registration = signUp(username, "TestPassword123!");
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = signIn(username, "TestPassword123!");
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(id(response), JsonPath.read(response.body(), "$.token"));
    }

    /** Creates an account with an active subscription and its own laboratory. */
    protected ReadyAccount readyAccount() throws Exception {
        var account = account();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(30);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(account.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_it_" + unique, "sub_it_" + unique,
                "cs_it_" + unique, end.minusMonths(1).toString(), end.toString())));
        var laboratory = call("POST", "/laboratories", account.token(), """
                {"name":"Integration laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(laboratory.statusCode()).withFailMessage(laboratory.body()).isEqualTo(201);
        long laboratoryId = id(laboratory);
        var environment = call("POST", "/laboratories/" + laboratoryId + "/environments", account.token(),
                "{\"code\":\"ENV-01\",\"name\":\"Integration environment\"}");
        assertThat(environment.statusCode()).withFailMessage(environment.body()).isEqualTo(201);
        return new ReadyAccount(account, laboratoryId, id(environment));
    }

    /** Registers a staff member the way a quality manager does and signs them in after the password change. */
    protected Staff staff(ReadyAccount owner, String fullName, String accessRole) throws Exception {
        var email = "staff-" + UUID.randomUUID() + "@qualitrack.test";
        var created = call("POST", "/laboratories/" + owner.laboratoryId() + "/staff", owner.token(), """
                {"fullName":"%s","role":"Production operator","email":"%s","accessRole":"%s"}
                """.formatted(fullName, email, accessRole));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        String temporaryPassword = json(created, "$.credentials.temporaryPassword");
        var session = signIn(email, temporaryPassword);
        assertThat(session.statusCode()).withFailMessage(session.body()).isEqualTo(200);
        String token = json(session, "$.token");
        var changed = call("POST", "/users/me/password-changes", token, """
                {"currentPassword":"%s","newPassword":"StaffPassword123"}
                """.formatted(temporaryPassword));
        assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(204);
        return new Staff(((Number) json(created, "$.staffMember.id")).longValue(),
                ((Number) json(created, "$.staffMember.userId")).longValue(), token, fullName);
    }

    /** Registers equipment in the laboratory and, when asked, locates it in the environment. */
    protected HttpResponse<String> registerEquipment(ReadyAccount actor, long laboratoryId, String name) throws Exception {
        return call("POST", "/laboratories/" + laboratoryId + "/equipments", actor.token(), """
                {"name":"%s","type":"Refrigerator","model":"R-200","serialNumber":"SN-%s"}
                """.formatted(name, UUID.randomUUID()));
    }

    protected long locatedEquipment(ReadyAccount owner, String name) throws Exception {
        var created = registerEquipment(owner, owner.laboratoryId(), name);
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long equipmentId = id(created);
        var located = call("POST", environmentPath(owner) + "/equipments", owner.token(),
                "{\"equipmentId\":" + equipmentId + "}");
        assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
        return equipmentId;
    }

    /** Registers the environmental device of the owner's environment and associates it with the environment. */
    protected long environmentalDevice(ReadyAccount owner) throws Exception {
        var created = call("POST", "/laboratories/" + owner.laboratoryId() + "/devices/environmental-devices", owner.token(), """
                {"name":"Space monitor","sensorExternalId":"ENV-%1$s","serialNumber":"MAC-%1$s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8)));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long deviceId = id(created);
        var associated = call("POST", environmentPath(owner) + "/environmental-devices", owner.token(),
                "{\"deviceId\":" + deviceId + "}");
        assertThat(associated.statusCode()).withFailMessage(associated.body()).isEqualTo(201);
        return deviceId;
    }

    protected static String environmentPath(ReadyAccount owner) {
        return "/laboratories/" + owner.laboratoryId() + "/environments/" + owner.environmentId();
    }

    protected HttpResponse<String> signUp(String username, String password) throws Exception {
        return call("POST", "/authentication/sign-up", null, """
                {"username":"%s","email":"%s@qualitrack.test","password":"%s","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username, username, password));
    }

    protected HttpResponse<String> signIn(String username, String password) throws Exception {
        return call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"%s"}
                """.formatted(username, password));
    }

    protected HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected static long id(HttpResponse<String> response) {
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    protected static <T> T json(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }
}
