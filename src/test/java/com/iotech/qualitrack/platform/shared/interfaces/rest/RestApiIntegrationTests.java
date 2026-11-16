package com.iotech.qualitrack.platform.shared.interfaces.rest;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the REST conventions of the endpoints reviewed before closing phase 5: actions registered as sub-resources
 * with the authenticated user as actor, nested routes, 201 with Location and error bodies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestApiIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired DeviationAlertRepository alerts;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20940000000L);

    private record Account(Long id, String token) { }

    private record Plant(Account manager, long lab) {
        String equipment(long equipmentId) { return "/laboratories/" + lab + "/equipments/" + equipmentId; }
    }

    @Test
    void alertsAreAcknowledgedAndResolvedAsSubResourcesByTheAuthenticatedUser() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long equipmentId = equipment(plant);
        var alert = alerts.save(new DeviationAlert(null, equipmentId, null, "TEMPERATURE", 9.3, 8.0, "C",
                "2026-09-01T12:00:00", AlertSeverity.CRITICAL, AlertStatus.UNRESOLVED, null, null, null));

        // Alerts are listed per environment (TS74); the equipment route was replaced.
        assertThat(call("GET", plant.equipment(equipmentId) + "/deviation-alerts", token, null).statusCode()).isEqualTo(404);

        var acknowledged = call("POST", "/deviation-alerts/" + alert.getId() + "/acknowledgements", token, null);
        assertThat(acknowledged.statusCode()).withFailMessage(acknowledged.body()).isEqualTo(201);
        assertThat(acknowledged.body()).contains("\"status\":\"ACKNOWLEDGED\"", "\"acknowledgedBy\":" + plant.manager().id(),
                "\"acknowledgedAt\":\"");
        assertThat(call("POST", "/deviation-alerts/" + alert.getId() + "/acknowledgements", token, null).statusCode()).isEqualTo(409);

        assertThat(call("POST", "/deviation-alerts/" + alert.getId() + "/resolutions", token, "{}").statusCode()).isEqualTo(400);
        var resolved = call("POST", "/deviation-alerts/" + alert.getId() + "/resolutions", token,
                "{\"resolutionNotes\":\"Equipment recalibrated\"}");
        assertThat(resolved.statusCode()).withFailMessage(resolved.body()).isEqualTo(201);
        assertThat(resolved.body()).contains("\"status\":\"RESOLVED\"", "\"resolvedBy\":" + plant.manager().id(),
                "\"resolvedAt\":\"");
        assertThat(call("POST", "/deviation-alerts/" + alert.getId() + "/resolutions", token,
                "{\"resolutionNotes\":\"Again\"}").statusCode()).isEqualTo(409);

        assertThat(call("GET", "/deviation-alerts/" + alert.getId(), token, null).statusCode()).isEqualTo(200);
        var other = plant();
        assertThat(call("POST", "/deviation-alerts/" + alert.getId() + "/acknowledgements", other.manager().token(), null)
                .statusCode()).isEqualTo(403);
        var patch = call("PATCH", "/deviation-alerts/" + alert.getId(), token, "{\"status\":\"RESOLVED\"}");
        assertThat(patch.statusCode()).isEqualTo(405);
        assertThat(patch.headers().firstValue("Allow")).hasValueSatisfying(allow -> assertThat(allow).contains("GET"));
        var removed = call("GET", "/equipments/" + equipmentId + "/deviation-alerts", token, null);
        assertThat(removed.statusCode()).isEqualTo(404);
        assertThat(removed.body()).contains("\"code\"");
    }

    @Test
    void bpmParameterRangesAreResourcesOfTheEquipmentIdentifiedByTheirName() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long equipmentId = equipment(plant);
        var configs = plant.equipment(equipmentId) + "/bpm-configs";

        var saved = call("PUT", configs + "/TEMPERATURE", token, "{\"minValue\":2.0,\"maxValue\":8.0,\"unit\":\"C\"}");
        assertThat(saved.statusCode()).withFailMessage(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"parameterName\":\"TEMPERATURE\"", "\"maxValue\":8.0");
        var replaced = call("PUT", configs + "/TEMPERATURE", token, "{\"minValue\":2.0,\"maxValue\":6.0,\"unit\":\"C\"}");
        assertThat(replaced.statusCode()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(replaced.body(), "$.id")).longValue())
                .isEqualTo(((Number) JsonPath.read(saved.body(), "$.id")).longValue());

        assertThat(call("GET", configs, token, null).body()).contains("\"maxValue\":6.0");
        assertThat(call("GET", configs + "/TEMPERATURE", token, null).statusCode()).isEqualTo(200);
        var missing = call("GET", configs + "/PRESSURE", token, null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.body()).contains("\"code\"");
        assertThat(call("PUT", configs + "/PRESSURE", token, "{\"minValue\":5.0,\"maxValue\":1.0,\"unit\":\"bar\"}")
                .statusCode()).isEqualTo(400);
        assertThat(call("GET", configs, plant().manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void createdLaboratoriesAndUsersAnswerWithTheirLocation() throws Exception {
        var username = "rest-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username));
        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(registration.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/api/v1/users/" + id(registration)));

        var plant = plant();
        var laboratory = call("GET", "/laboratories/" + plant.lab(), plant.manager().token(), null);
        assertThat(laboratory.statusCode()).isEqualTo(200);
        var subscriptionsOfLab = call("GET", "/laboratories/" + plant.lab() + "/subscriptions?status=ACTIVE",
                plant.manager().token(), null);
        assertThat(subscriptionsOfLab.statusCode()).isEqualTo(200);
        assertThat(subscriptionsOfLab.body()).startsWith("[").contains("\"status\":\"ACTIVE\"");
        assertThat(call("GET", "/laboratories/" + plant.lab() + "/subscriptions?status=CANCELLED",
                plant.manager().token(), null).body()).isEqualTo("[]");
    }

    private long equipment(Plant plant) throws Exception {
        var created = call("POST", "/laboratories/" + plant.lab() + "/equipments", plant.manager().token(), """
                {"name":"Stability chamber","type":"Chamber","model":"SC-1","serialNumber":"SN-%s"}
                """.formatted(UUID.randomUUID()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private Plant plant() throws Exception {
        var manager = account();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"REST laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/api/v1/laboratories/" + lab));
        return new Plant(manager, lab);
    }

    private Account account() throws Exception {
        var username = "rest-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), JsonPath.read(response.body(), "$.token"));
    }

    private static long id(HttpResponse<String> response) {
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
