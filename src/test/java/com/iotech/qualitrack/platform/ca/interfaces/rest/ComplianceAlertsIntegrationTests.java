package com.iotech.qualitrack.platform.ca.interfaces.rest;

import com.iotech.qualitrack.platform.testing.TestStaff;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deviation alerts of the environments and monitored containers of a laboratory (EP09: US85-US88, TS73-TS76).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComplianceAlertsIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20990000000L);

    private record Account(Long id, String token) { }

    private record Lab(Account manager, long id, long environment, long spaceMonitor, long containerMonitor) {
        String environmentPath() { return "/laboratories/" + id + "/environments/" + environment; }
        String container() { return environmentPath() + "/container-monitors/" + containerMonitor; }
        String alerts() { return environmentPath() + "/deviation-alerts"; }
    }

    @Test
    void anIncidentDetectedByTrackingIsOneAlertWithItsActionsUntilItIsResolved() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        assertThat(call("PUT", lab.container() + "/environmental-profile/thresholds", token, """
                {"thresholds":[{"metric":"TEMPERATURE","normalMin":15,"normalMax":25,"criticalMin":8,"criticalMax":30},
                               {"metric":"HUMIDITY","normalMin":40,"normalMax":60,"criticalMin":30,"criticalMax":70}]}
                """).statusCode()).isEqualTo(200);
        var readings = lab.container() + "/telemetry-measurements";
        var actions = lab.container() + "/actuation-events";
        var start = Instant.now().minus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        assertThat(JsonPath.<List<Object>>read(call("GET", lab.alerts() + "?active=true", token, null).body(), "$")).isEmpty();
        call("POST", readings, token, reading("TEMPERATURE", 20.0, start));
        call("POST", readings, token, reading("TEMPERATURE", 27.0, start.plusSeconds(60)));
        call("POST", actions, token, action("VENTILATION_ON", "TEMPERATURE", "WARNING", start.plusSeconds(60)));
        call("POST", actions, token, action("VENTILATION_ON", "HUMIDITY", "WARNING", start.plusSeconds(90)));
        call("POST", readings, token, reading("TEMPERATURE", 33.0, start.plusSeconds(120)));
        call("POST", actions, token, action("COOLING_ON", "TEMPERATURE", "CRITICAL", start.plusSeconds(120)));

        var active = call("GET", lab.alerts() + "?active=true", token, null);
        assertThat(active.statusCode()).withFailMessage(active.body()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(active.body(), "$[*].id")).hasSize(1);
        assertThat(active.body()).contains("\"severity\":\"CRITICAL\"", "\"status\":\"UNRESOLVED\"", "\"origin\":\"CONTAINER\"",
                "\"deviationCount\":2", "\"recordedValue\":33.0", "\"thresholdValue\":30.0", "\"parameterName\":\"TEMPERATURE\"",
                "\"environmentId\":" + lab.environment(), "\"normalizedAt\":null");
        long alertId = ((Number) JsonPath.read(active.body(), "$[0].id")).longValue();
        assertThat((Object) JsonPath.read(active.body(), "$[0].measurementId")).isNotNull();

        call("POST", readings, token, reading("TEMPERATURE", 22.0, start.plusSeconds(180)));
        var detail = call("GET", "/deviation-alerts/" + alertId, token, null);
        assertThat(detail.statusCode()).withFailMessage(detail.body()).isEqualTo(200);
        assertThat(detail.body()).contains("\"status\":\"UNRESOLVED\"").doesNotContain("\"normalizedAt\":null");
        assertThat(JsonPath.<List<String>>read(detail.body(), "$.relatedActuations[*].action"))
                .containsExactly("VENTILATION_ON", "COOLING_ON");
        assertThat(JsonPath.<List<String>>read(detail.body(), "$.relatedActuations[*].triggerState"))
                .containsExactly("WARNING", "CRITICAL");
        assertThat(call("GET", "/deviation-alerts/999999999", token, null).statusCode()).isIn(403, 404);

        var auditor = TestStaff.register(this::call, lab.id(), token, "Alert auditor", "AUDITOR");
        assertThat(call("GET", "/deviation-alerts/" + alertId, auditor.token(), null).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/deviation-alerts/" + alertId + "/acknowledgements", auditor.token(), null).statusCode())
                .isEqualTo(403);

        var operator = TestStaff.register(this::call, lab.id(), token, "Alert operator", "OPERATOR");
        var acknowledged = call("POST", "/deviation-alerts/" + alertId + "/acknowledgements", operator.token(), null);
        assertThat(acknowledged.statusCode()).withFailMessage(acknowledged.body()).isEqualTo(201);
        assertThat(acknowledged.body()).contains("\"status\":\"ACKNOWLEDGED\"", "\"acknowledgedBy\":" + operator.userId());
        var resolved = call("POST", "/deviation-alerts/" + alertId + "/resolutions", operator.token(),
                "{\"resolutionNotes\":\"Cabinet door closed and cooling verified\"}");
        assertThat(resolved.statusCode()).withFailMessage(resolved.body()).isEqualTo(201);
        assertThat(resolved.body()).contains("\"status\":\"RESOLVED\"", "\"resolvedBy\":" + operator.userId());

        // Once the incident is resolved, a new deviation opens a new alert.
        call("POST", readings, token, reading("TEMPERATURE", 27.0, start.plusSeconds(240)));
        var all = call("GET", lab.alerts(), token, null);
        assertThat(JsonPath.<List<String>>read(all.body(), "$[*].status")).containsExactly("UNRESOLVED", "RESOLVED");
        assertThat(JsonPath.<List<String>>read(call("GET", lab.alerts() + "?active=true", token, null).body(), "$[*].severity"))
                .containsExactly("WARNING");
        assertThat(JsonPath.<List<Integer>>read(call("GET", lab.alerts() + "?status=RESOLVED", token, null).body(), "$[*].id"))
                .containsExactly((int) alertId);
        assertThat(JsonPath.<List<Object>>read(call("GET", lab.alerts() + "?severity=CRITICAL&active=true", token, null).body(), "$"))
                .isEmpty();
        var invalid = call("GET", lab.alerts() + "?status=OPENED", token, null);
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("\"code\"");

        var foreign = laboratory();
        assertThat(call("GET", lab.alerts(), foreign.manager().token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/deviation-alerts/" + alertId, foreign.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void deviationsConfirmedThroughTheApiOpenOrJoinTheAlertOfTheirDevice() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var detectedAt = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        var opened = call("POST", lab.alerts(), token, deviation(null, "AIR_QUALITY", 850.0, 800.0, "ppm", "WARNING", detectedAt));
        assertThat(opened.statusCode()).withFailMessage(opened.body()).isEqualTo(201);
        long alertId = ((Number) JsonPath.read(opened.body(), "$.id")).longValue();
        assertThat(opened.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/api/v1/deviation-alerts/" + alertId));
        assertThat(opened.body()).contains("\"origin\":\"ENVIRONMENT\"", "\"equipmentId\":" + lab.spaceMonitor(),
                "\"laboratoryId\":" + lab.id());

        var joined = call("POST", lab.alerts(), token,
                deviation(lab.spaceMonitor(), "air_quality", 1300.0, 1200.0, "ppm", "CRITICAL", detectedAt.plusSeconds(60)));
        assertThat(joined.statusCode()).withFailMessage(joined.body()).isEqualTo(200);
        assertThat(joined.body()).contains("\"id\":" + alertId, "\"severity\":\"CRITICAL\"", "\"deviationCount\":2",
                "\"recordedValue\":1300.0");
        var milder = call("POST", lab.alerts(), token,
                deviation(lab.spaceMonitor(), "AIR_QUALITY", 900.0, 800.0, "ppm", "WARNING", detectedAt.plusSeconds(120)));
        assertThat(milder.statusCode()).isEqualTo(200);
        assertThat(milder.body()).contains("\"severity\":\"CRITICAL\"", "\"deviationCount\":3", "\"recordedValue\":1300.0");

        var container = call("POST", lab.alerts(), token,
                deviation(lab.containerMonitor(), "HUMIDITY", 75.0, 70.0, "%", "CRITICAL", detectedAt));
        assertThat(container.statusCode()).withFailMessage(container.body()).isEqualTo(201);
        assertThat(container.body()).contains("\"origin\":\"CONTAINER\"");

        var otherEnvironment = environment(lab, "QC-" + suffix());
        assertThat(call("POST", "/laboratories/" + lab.id() + "/environments/" + otherEnvironment + "/deviation-alerts", token,
                deviation(lab.containerMonitor(), "HUMIDITY", 75.0, 70.0, "%", "CRITICAL", detectedAt)).statusCode()).isEqualTo(400);
        var noDevice = call("POST", "/laboratories/" + lab.id() + "/environments/" + otherEnvironment + "/deviation-alerts", token,
                deviation(null, "AIR_QUALITY", 850.0, 800.0, "ppm", "WARNING", detectedAt));
        assertThat(noDevice.statusCode()).isEqualTo(400);
        assertThat(noDevice.body()).contains("environmental device");
        assertThat(call("POST", lab.alerts(), token, """
                {"recordedValue":850,"thresholdValue":800,"unit":"ppm","severity":"WARNING","detectedAt":"%s"}
                """.formatted(detectedAt)).statusCode()).isEqualTo(400);
        assertThat(call("POST", lab.alerts(), token,
                deviation(null, "AIR_QUALITY", 850.0, 800.0, "ppm", "SEVERE", detectedAt)).statusCode()).isEqualTo(400);

        var auditor = TestStaff.register(this::call, lab.id(), token, "Deviation auditor", "AUDITOR");
        assertThat(call("POST", lab.alerts(), auditor.token(),
                deviation(null, "AIR_QUALITY", 850.0, 800.0, "ppm", "WARNING", detectedAt)).statusCode()).isEqualTo(403);
        assertThat(call("GET", lab.alerts(), auditor.token(), null).statusCode()).isEqualTo(200);
        var foreign = laboratory();
        assertThat(call("POST", lab.alerts(), foreign.manager().token(),
                deviation(null, "AIR_QUALITY", 850.0, 800.0, "ppm", "WARNING", detectedAt)).statusCode()).isEqualTo(403);

        assertThat(JsonPath.<List<Integer>>read(call("GET", lab.alerts() + "?deviceId=" + lab.spaceMonitor(), token, null).body(),
                "$[*].id")).containsExactly((int) alertId);
    }

    private static String reading(String metric, double value, Instant measuredAt) {
        return """
                {"metric":"%s","value":%s,"measuredAt":"%s"}
                """.formatted(metric, value, measuredAt);
    }

    private static String action(String action, String metric, String state, Instant occurredAt) {
        return """
                {"action":"%s","triggerMetric":"%s","triggerState":"%s","occurredAt":"%s"}
                """.formatted(action, metric, state, occurredAt);
    }

    private static String deviation(Long deviceId, String parameter, double value, double threshold, String unit,
                                    String severity, Instant detectedAt) {
        return """
                {"deviceId":%s,"parameterName":"%s","recordedValue":%s,"thresholdValue":%s,"unit":"%s","severity":"%s","detectedAt":"%s"}
                """.formatted(deviceId, parameter, value, threshold, unit, severity, detectedAt);
    }

    private Lab laboratory() throws Exception {
        var manager = manager();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Compliance laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        var partial = new Lab(manager, lab, 0, 0, 0);
        long environment = environment(partial, "WH-" + suffix());
        var devices = "/laboratories/" + lab + "/devices";
        var environmentPath = "/laboratories/" + lab + "/environments/" + environment;
        long spaceMonitor = id(call("POST", devices + "/environmental-devices", manager.token(), device("Space monitor", "ENV-" + suffix())));
        assertThat(call("POST", environmentPath + "/environmental-devices", manager.token(), "{\"deviceId\":" + spaceMonitor + "}")
                .statusCode()).isEqualTo(201);
        long containerMonitor = id(call("POST", devices + "/container-monitors", manager.token(), device("Cold cabinet monitor", "CNT-" + suffix())));
        assertThat(call("POST", environmentPath + "/container-monitors", manager.token(), "{\"deviceId\":" + containerMonitor + "}")
                .statusCode()).isEqualTo(201);
        return new Lab(manager, lab, environment, spaceMonitor, containerMonitor);
    }

    private long environment(Lab lab, String code) throws Exception {
        var created = call("POST", "/laboratories/" + lab.id() + "/environments", lab.manager().token(),
                "{\"code\":\"" + code + "\",\"name\":\"Zone " + code + "\"}");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private static String device(String name, String identity) {
        return """
                {"name":"%s","sensorExternalId":"%s","serialNumber":"MAC-%s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(name, identity, suffix());
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Account manager() throws Exception {
        var username = "compliance-" + UUID.randomUUID();
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
