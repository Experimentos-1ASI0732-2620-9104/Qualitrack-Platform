package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.testing.TestStaff;
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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Environmental profiles, telemetry and actuation events of the IoT devices of an environment (EP07).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrackingIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AuditLogRepository auditLogs;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20950000000L);

    private record Account(Long id, String token) { }

    private record Lab(Account manager, long id, long environment, long spaceMonitor, long containerMonitor) {
        String environmentPath() { return "/laboratories/" + id + "/environments/" + environment; }
        String container() { return environmentPath() + "/container-monitors/" + containerMonitor; }
    }

    @Test
    void aQualityManagerConfiguresTheAirQualityOfAnEnvironmentWithItsEnvironmentalDevice() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var thresholds = lab.environmentPath() + "/environmental-profile/thresholds";
        assertThat(call("GET", lab.environmentPath() + "/environmental-profile", token, null).statusCode()).isEqualTo(404);

        var bare = environment(lab, "QC-" + suffix());
        var withoutDevice = call("PUT", "/laboratories/" + lab.id() + "/environments/" + bare + "/environmental-profile/thresholds",
                token, airQuality(400, 800));
        assertThat(withoutDevice.statusCode()).isEqualTo(400);
        assertThat(withoutDevice.body()).contains("environmental device");

        var saved = call("PUT", thresholds, token, airQuality(400, 800));
        assertThat(saved.statusCode()).withFailMessage(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"scope\":\"ENVIRONMENT\"").contains("\"version\":1").contains("\"unit\":\"ppm\"");
        assertThat(call("PUT", thresholds, token, airQuality(800, 400)).statusCode()).isEqualTo(400);
        assertThat(call("PUT", thresholds, token, """
                {"thresholds":[{"metric":"TEMPERATURE","normalMin":15,"normalMax":25,"criticalMin":8,"criticalMax":30}]}
                """).statusCode()).isEqualTo(400);
        assertThat(call("PUT", thresholds, token, """
                {"thresholds":[{"metric":"AIR_QUALITY","normalMax":400}]}
                """).statusCode()).isEqualTo(400);
        assertThat(call("PUT", thresholds, token, """
                {"thresholds":[{"metric":"NOISE","normalMax":40,"criticalMax":60}]}
                """).statusCode()).isEqualTo(400);

        var operator = TestStaff.register(this::call, lab.id(), token, "Environment operator", "OPERATOR");
        assertThat(call("PUT", thresholds, operator.token(), airQuality(300, 700)).statusCode()).isEqualTo(403);
        var profile = call("GET", lab.environmentPath() + "/environmental-profile", operator.token(), null);
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(profile.body(), "$.version")).isEqualTo(1);
        assertThat(JsonPath.<Double>read(profile.body(), "$.thresholds[0].criticalMax")).isEqualTo(800.0);

        var edgeProfile = call("GET", lab.environmentPath() + "/devices/" + lab.spaceMonitor() + "/environmental-profile", token, null);
        assertThat(edgeProfile.statusCode()).isEqualTo(200);
        assertThat(edgeProfile.body()).contains("\"scope\":\"ENVIRONMENT\"").contains("\"version\":1");

        long profileId = id(saved);
        assertThat(auditLogs.findAllByPerformedBy(lab.manager().id()))
                .anyMatch(entry -> "ENVIRONMENTAL_PROFILE".equals(entry.getEntityType()) && entry.getEntityId() == profileId);
    }

    @Test
    void aContainerMonitorHasThresholdsAndActuationRulesThatNeedThem() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var profilePath = lab.container() + "/environmental-profile";
        var rules = profilePath + "/actuation-rules";
        assertThat(call("PUT", rules, token, ventilateOnWarning()).statusCode()).isEqualTo(400);

        var saved = call("PUT", profilePath + "/thresholds", token, containerThresholds());
        assertThat(saved.statusCode()).withFailMessage(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"scope\":\"CONTAINER_MONITOR\"").contains("\"version\":1")
                .contains("\"unit\":\"°C\"").contains("\"unit\":\"%RH\"");
        var withRules = call("PUT", rules, token, ventilateOnWarning());
        assertThat(withRules.statusCode()).withFailMessage(withRules.body()).isEqualTo(200);
        assertThat(withRules.body()).contains("\"version\":2").contains("\"action\":\"VENTILATION_ON\"");

        assertThat(call("PUT", rules, token, """
                {"rules":[{"metric":"LUMINOSITY","state":"WARNING","action":"SERVO_OPEN"}]}
                """).statusCode()).isEqualTo(400);
        assertThat(call("PUT", rules, token, """
                {"rules":[{"metric":"TEMPERATURE","state":"WARNING","action":"VENTILATION_OFF"}]}
                """).statusCode()).isEqualTo(400);
        var dropped = call("PUT", profilePath + "/thresholds", token, """
                {"thresholds":[{"metric":"HUMIDITY","normalMin":40,"normalMax":60,"criticalMin":30,"criticalMax":70}]}
                """);
        assertThat(dropped.statusCode()).isEqualTo(400);
        assertThat(dropped.body()).contains("needs a threshold");
        assertThat(call("PUT", profilePath + "/thresholds", token, """
                {"thresholds":[{"metric":"AIR_QUALITY","normalMax":400,"criticalMax":800}]}
                """).statusCode()).isEqualTo(400);

        var notAContainer = "/laboratories/" + lab.id() + "/environments/" + lab.environment() + "/container-monitors/"
                + lab.spaceMonitor() + "/environmental-profile";
        assertThat(call("PUT", notAContainer + "/thresholds", token, containerThresholds()).statusCode()).isEqualTo(400);
        assertThat(call("GET", notAContainer, token, null).statusCode()).isEqualTo(404);

        var edgeProfile = call("GET", lab.environmentPath() + "/devices/" + lab.containerMonitor() + "/environmental-profile", token, null);
        assertThat(edgeProfile.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(edgeProfile.body(), "$.version")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(edgeProfile.body(), "$.actuationRules[*].action"))
                .containsExactly("VENTILATION_ON", "COOLING_ON");
    }

    @Test
    void readingsAreEvaluatedAndTheDeviationsOfAnIncidentShareOneAlert() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        assertThat(call("PUT", lab.container() + "/environmental-profile/thresholds", token, containerThresholds()).statusCode())
                .isEqualTo(200);
        var readings = lab.container() + "/telemetry-measurements";
        var start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        var normal = call("POST", readings, token, reading("TEMPERATURE", 20.0, start));
        assertThat(normal.statusCode()).withFailMessage(normal.body()).isEqualTo(201);
        assertThat(normal.body()).contains("\"state\":\"NORMAL\"").contains("\"unit\":\"°C\"").contains("\"profileVersion\":1");
        var again = call("POST", readings, token, reading("TEMPERATURE", 20.0, start));
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(id(again)).isEqualTo(id(normal));

        var warning = call("POST", readings, token, reading("TEMPERATURE", 27.0, start.plusSeconds(60)));
        assertThat(warning.body()).contains("\"state\":\"WARNING\"").contains("\"thresholdValue\":25.0");
        call("POST", readings, token, reading("TEMPERATURE", 28.0, start.plusSeconds(120)));
        var critical = call("POST", readings, token, reading("TEMPERATURE", 33.0, start.plusSeconds(180)));
        assertThat(critical.body()).contains("\"state\":\"CRITICAL\"").contains("\"thresholdValue\":30.0");
        call("POST", readings, token, reading("TEMPERATURE", 34.0, start.plusSeconds(240)));
        call("POST", readings, token, reading("TEMPERATURE", 22.0, start.plusSeconds(300)));
        call("POST", readings, token, reading("TEMPERATURE", 26.0, start.plusSeconds(360)));

        // WARNING, CRITICAL, back to normal and WARNING again are one incident while the alert is open.
        var alerts = call("GET", lab.environmentPath() + "/deviation-alerts?deviceId=" + lab.containerMonitor(), token, null);
        assertThat(alerts.statusCode()).withFailMessage(alerts.body()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(alerts.body(), "$[*].severity")).containsExactly("CRITICAL");
        assertThat(JsonPath.<List<Integer>>read(alerts.body(), "$[*].deviationCount")).containsExactly(3);
        assertThat(JsonPath.<List<Double>>read(alerts.body(), "$[*].thresholdValue")).containsExactly(30.0);
        assertThat(JsonPath.<List<Double>>read(alerts.body(), "$[*].recordedValue")).containsExactly(33.0);
        assertThat(JsonPath.<List<String>>read(alerts.body(), "$[*].origin")).containsExactly("CONTAINER");
        assertThat(JsonPath.<List<Object>>read(alerts.body(), "$[*].normalizedAt")).containsExactly((Object) null);
        assertThat(JsonPath.<List<String>>read(alerts.body(), "$[*].unit")).containsOnly("°C");

        var rfid = call("POST", readings, token, """
                {"metric":"RFID_TAG","textValue":"E200-3412-0001","measuredAt":"%s"}
                """.formatted(start.plusSeconds(30)));
        assertThat(rfid.statusCode()).withFailMessage(rfid.body()).isEqualTo(201);
        assertThat(rfid.body()).contains("\"textValue\":\"E200-3412-0001\"").contains("\"state\":null");
        assertThat(call("POST", readings, token, reading("AIR_QUALITY", 300.0, start)).statusCode()).isEqualTo(400);
        assertThat(call("POST", readings, token, reading("TEMPERATURE", 20.0, Instant.now().plus(1, ChronoUnit.HOURS)))
                .statusCode()).isEqualTo(400);
        assertThat(call("POST", readings, token, """
                {"metric":"TEMPERATURE","value":20,"measuredAt":"yesterday"}
                """).statusCode()).isEqualTo(400);

        var history = call("GET", readings + "?metric=TEMPERATURE&from=" + encode(start.minusSeconds(1)) + "&to="
                + encode(Instant.now()), token, null);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<Double>>read(history.body(), "$[*].value"))
                .containsExactly(20.0, 27.0, 28.0, 33.0, 34.0, 22.0, 26.0);
        assertThat(JsonPath.<List<String>>read(call("GET", readings, token, null).body(), "$[*].metric"))
                .contains("TEMPERATURE", "RFID_TAG");
        assertThat(call("GET", readings + "?from=" + encode(Instant.now()) + "&to=" + encode(start), token, null).statusCode())
                .isEqualTo(400);

        var status = call("GET", lab.environmentPath() + "/devices/" + lab.containerMonitor() + "/telemetry-status", token, null);
        assertThat(status.body()).contains("\"connectionStatus\":\"CONNECTED\"");
    }

    @Test
    void theEnvironmentalDeviceReportsAirQualityAndMotion() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var operator = TestStaff.register(this::call, lab.id(), token, "Air operator", "OPERATOR");
        var readings = lab.environmentPath() + "/telemetry-measurements";
        var start = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        var unconfigured = call("POST", readings, operator.token(), reading("AIR_QUALITY", 900.0, start));
        assertThat(unconfigured.statusCode()).withFailMessage(unconfigured.body()).isEqualTo(201);
        assertThat(unconfigured.body()).contains("\"state\":null").contains("\"unit\":\"ppm\"");
        assertThat(call("PUT", lab.environmentPath() + "/environmental-profile/thresholds", token, airQuality(400, 800)).statusCode())
                .isEqualTo(200);
        var polluted = call("POST", readings, operator.token(), reading("AIR_QUALITY", 900.0, start.plusSeconds(60)));
        assertThat(polluted.body()).contains("\"state\":\"CRITICAL\"").contains("\"thresholdValue\":800.0");
        var motion = call("POST", readings, operator.token(), reading("MOTION", 1.0, start.plusSeconds(90)));
        assertThat(motion.statusCode()).isEqualTo(201);
        assertThat(motion.body()).contains("\"unit\":\"event\"");
        assertThat(call("POST", readings, operator.token(), reading("MOTION", 3.0, start.plusSeconds(95))).statusCode()).isEqualTo(400);
        assertThat(call("POST", readings, operator.token(), reading("TEMPERATURE", 20.0, start)).statusCode()).isEqualTo(400);

        var auditor = TestStaff.register(this::call, lab.id(), token, "Air auditor", "AUDITOR");
        assertThat(call("POST", readings, auditor.token(), reading("AIR_QUALITY", 100.0, start.plusSeconds(120))).statusCode())
                .isEqualTo(403);
        var listed = call("GET", readings + "?metric=MOTION", auditor.token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<Double>>read(listed.body(), "$[*].value")).containsExactly(1.0);

        var alerts = call("GET", lab.environmentPath() + "/deviation-alerts?deviceId=" + lab.spaceMonitor(), token, null);
        assertThat(JsonPath.<List<String>>read(alerts.body(), "$[*].severity")).containsExactly("CRITICAL");
        assertThat(JsonPath.<List<String>>read(alerts.body(), "$[*].origin")).containsExactly("ENVIRONMENT");

        var bare = environment(lab, "QC-" + suffix());
        var bareReadings = "/laboratories/" + lab.id() + "/environments/" + bare + "/telemetry-measurements";
        assertThat(call("POST", bareReadings, token, reading("AIR_QUALITY", 100.0, start)).statusCode()).isEqualTo(400);
        assertThat(call("GET", bareReadings, token, null).statusCode()).isEqualTo(404);
        var foreign = laboratory();
        assertThat(call("GET", readings, foreign.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void containerMonitorsReportTheActionsTheyExecuted() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var events = lab.container() + "/actuation-events";
        var start = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        assertThat(JsonPath.<List<Object>>read(call("GET", events, token, null).body(), "$")).isEmpty();

        var started = call("POST", events, token, """
                {"action":"VENTILATION_ON","triggerMetric":"TEMPERATURE","triggerState":"WARNING","occurredAt":"%s","profileVersion":2}
                """.formatted(start));
        assertThat(started.statusCode()).withFailMessage(started.body()).isEqualTo(201);
        assertThat(started.body()).contains("\"result\":\"EXECUTED\"").contains("\"profileVersion\":2");
        assertThat(call("POST", events, token, """
                {"action":"VENTILATION_ON","triggerMetric":"TEMPERATURE","triggerState":"WARNING","occurredAt":"%s"}
                """.formatted(start)).statusCode()).isEqualTo(200);
        assertThat(call("POST", events, token, """
                {"action":"VENTILATION_OFF","triggerMetric":"TEMPERATURE","triggerState":"NORMAL","occurredAt":"%s"}
                """.formatted(start.plusSeconds(120))).statusCode()).isEqualTo(201);
        assertThat(call("POST", events, token, """
                {"action":"SERVO_OPEN","triggerMetric":"TEMPERATURE","occurredAt":"%s"}
                """.formatted(start)).statusCode()).isEqualTo(400);
        assertThat(call("POST", events, token, """
                {"action":"SERVO_OPEN","triggerMetric":"AIR_QUALITY","triggerState":"WARNING","occurredAt":"%s"}
                """.formatted(start)).statusCode()).isEqualTo(400);
        assertThat(call("POST", events, token, """
                {"action":"FAN_ON","occurredAt":"%s"}
                """.formatted(start)).statusCode()).isEqualTo(400);

        var listed = call("GET", events, token, null);
        assertThat(JsonPath.<List<String>>read(listed.body(), "$[*].action")).containsExactly("VENTILATION_ON", "VENTILATION_OFF");
        var notAContainer = "/laboratories/" + lab.id() + "/environments/" + lab.environment() + "/container-monitors/"
                + lab.spaceMonitor() + "/actuation-events";
        assertThat(call("GET", notAContainer, token, null).statusCode()).isEqualTo(404);
        assertThat(call("POST", notAContainer, token, """
                {"action":"SERVO_OPEN","occurredAt":"%s"}
                """.formatted(start)).statusCode()).isEqualTo(400);
    }

    private static String airQuality(double normalMax, double criticalMax) {
        return """
                {"thresholds":[{"metric":"AIR_QUALITY","normalMax":%s,"criticalMax":%s}]}
                """.formatted(normalMax, criticalMax);
    }

    private static String containerThresholds() {
        return """
                {"thresholds":[
                  {"metric":"TEMPERATURE","normalMin":15,"normalMax":25,"criticalMin":8,"criticalMax":30},
                  {"metric":"HUMIDITY","normalMin":40,"normalMax":60,"criticalMin":30,"criticalMax":70}]}
                """;
    }

    private static String ventilateOnWarning() {
        return """
                {"rules":[{"metric":"TEMPERATURE","state":"WARNING","action":"VENTILATION_ON"},
                          {"metric":"TEMPERATURE","state":"CRITICAL","action":"COOLING_ON"}]}
                """;
    }

    private static String reading(String metric, double value, Instant measuredAt) {
        return """
                {"metric":"%s","value":%s,"measuredAt":"%s"}
                """.formatted(metric, value, measuredAt);
    }

    private static String encode(Instant moment) {
        return URLEncoder.encode(moment.toString(), StandardCharsets.UTF_8);
    }

    private Lab laboratory() throws Exception {
        var manager = manager();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Tracking laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
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
        var username = "tracking-" + UUID.randomUUID();
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
