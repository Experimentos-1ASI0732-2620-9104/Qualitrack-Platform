package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Indicators and reports calculated from persisted records (EP10: US93-US97, TS81-TS85).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportingIndicatorsIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20960000000L);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private record Account(Long id, String token) { }

    private record Lab(Account manager, long id, long environment, long spaceMonitor, long containerMonitor) {
        String environmentPath() { return "/laboratories/" + id + "/environments/" + environment; }
        String container() { return environmentPath() + "/container-monitors/" + containerMonitor; }
    }

    @Test
    void indicatorsSummarizeTheReadingsOfThePeriodWithTimeInRangeAndDeviations() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var start = readTemperatureIncident(lab);
        var period = "from=" + encode(start.minusSeconds(60)) + "&to=" + encode(Instant.now());

        var dashboard = call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?" + period, token, null);
        assertThat(dashboard.statusCode()).withFailMessage(dashboard.body()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(dashboard.body(), "$.measurementSummaries[*].metric")).containsExactly("TEMPERATURE");
        assertThat(dashboard.body()).contains("\"readings\":5", "\"average\":24.6", "\"minimum\":20.0", "\"maximum\":33.0",
                "\"deviceId\":" + lab.containerMonitor(), "\"environmentId\":" + lab.environment());
        assertThat((String) JsonPath.read(dashboard.body(), "$.from")).isNotNull();
        var other = environment(lab, "QC-" + suffix());
        assertThat(JsonPath.<List<Object>>read(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?" + period
                + "&environmentId=" + other, token, null).body(), "$.measurementSummaries")).isEmpty();
        // An environment of no laboratory is rejected by the tenant check before the query.
        assertThat(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?environmentId=999999999", token, null)
                .statusCode()).isIn(403, 404);
        assertThat(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?from=yesterday", token, null).statusCode())
                .isEqualTo(400);
        assertThat(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?from=" + encode(Instant.now()) + "&to="
                + encode(start), token, null).statusCode()).isEqualTo(400);
        assertThat(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards?from="
                + encode(Instant.now().minus(40, ChronoUnit.DAYS)), token, null).statusCode()).isEqualTo(400);

        var trends = call("GET", lab.environmentPath() + "/deviation-trends?" + period, token, null);
        assertThat(trends.statusCode()).withFailMessage(trends.body()).isEqualTo(200);
        assertThat(JsonPath.<List<Double>>read(trends.body(), "$[*].timeInRangePercent")).containsExactly(80.0);
        assertThat(JsonPath.<List<Integer>>read(trends.body(), "$[*].deviationCount")).containsExactly(2);
        assertThat(JsonPath.<List<Integer>>read(trends.body(), "$[*].criticalDeviationCount")).containsExactly(1);
        assertThat(JsonPath.<List<String>>read(trends.body(), "$[0].dataPoints[*].state"))
                .containsExactly("NORMAL", "WARNING", "NORMAL", "NORMAL", "CRITICAL");
        assertThat(JsonPath.<List<Object>>read(call("GET", "/laboratories/" + lab.id() + "/environments/" + other
                + "/deviation-trends", token, null).body(), "$")).isEmpty();
        assertThat(call("GET", "/laboratories/" + lab.id() + "/equipments/" + lab.containerMonitor() + "/deviation-trends",
                token, null).statusCode()).isEqualTo(404);

        var auditor = TestStaff.register(this::call, lab.id(), token, "Indicators auditor", "AUDITOR");
        assertThat(call("GET", lab.environmentPath() + "/deviation-trends", auditor.token(), null).statusCode()).isEqualTo(200);
        var foreign = laboratory();
        assertThat(call("GET", "/laboratories/" + lab.id() + "/kpi-dashboards", foreign.manager().token(), null).statusCode())
                .isEqualTo(403);
        assertThat(call("GET", lab.environmentPath() + "/deviation-trends", foreign.manager().token(), null).statusCode())
                .isEqualTo(403);
    }

    @Test
    void theEnvironmentalReportCombinesIndicatorsAlertsAndActionsOfEachEnvironment() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var start = readTemperatureIncident(lab);
        assertThat(call("POST", lab.container() + "/actuation-events", token, """
                {"action":"COOLING_ON","triggerMetric":"TEMPERATURE","triggerState":"CRITICAL","occurredAt":"%s"}
                """.formatted(start.plusSeconds(3000))).statusCode()).isEqualTo(201);
        var today = LocalDate.now(LIMA);
        var body = """
                {"startDate":"%s","endDate":"%s","format":"CSV"}
                """.formatted(today.minusDays(1), today);

        var report = call("POST", "/laboratories/" + lab.id() + "/compliance-reports", token, body);
        assertThat(report.statusCode()).withFailMessage(report.body()).isEqualTo(201);
        var content = call("GET", "/reports/" + JsonPath.read(report.body(), "$.id") + "/content", token, null);
        assertThat(content.body()).contains("\"Time in range %\"", "\"Cold cabinet monitor\",\"TEMPERATURE\",\"°C\",\"5\",\"24.6\",\"20.0\",\"33.0\",\"80.0\",\"2\",\"1\"",
                "\"CONTAINER\",\"Cold cabinet monitor\"", "\"COOLING_ON\",\"TEMPERATURE\",\"CRITICAL\",\"EXECUTED\"");

        var single = call("POST", "/laboratories/" + lab.id() + "/compliance-reports", token, """
                {"environmentId":%d,"startDate":"%s","endDate":"%s","format":"PDF"}
                """.formatted(lab.environment(), today.minusDays(1), today));
        assertThat(single.statusCode()).isEqualTo(201);
        var pdf = binaryCall("/reports/" + JsonPath.read(single.body(), "$.id") + "/content", token);
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("Environmental report", "Cold cabinet monitor",
                    "80 %", "2 (1 critical)", "cooling on");
        }
        var foreign = laboratory();
        assertThat(call("POST", "/laboratories/" + lab.id() + "/compliance-reports", token, """
                {"environmentId":%d,"startDate":"%s","endDate":"%s","format":"CSV"}
                """.formatted(foreign.environment(), today, today)).statusCode()).isEqualTo(400);
    }

    @Test
    void theInventoryReportListsRawMaterialsLotsAndQuantities() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        assertThat(call("POST", lab.environmentPath() + "/usage-assignments", token, "{\"usage\":\"RAW_MATERIAL_STORAGE\"}")
                .statusCode()).isIn(200, 201);
        var materials = lab.environmentPath() + "/raw-materials";
        var material = call("POST", materials, token, """
                {"code":"PA-001","name":"Paracetamol API","unit":"kg","minimumStock":20}
                """);
        assertThat(material.statusCode()).withFailMessage(material.body()).isEqualTo(201);
        var today = LocalDate.now(LIMA);
        var lot = call("POST", materials + "/" + JsonPath.read(material.body(), "$.id") + "/batches", token, """
                {"supplier":"Quimica Andina","batchNumber":"LOT-INV-1","unit":"kg","amount":100,"receivedOn":"%s","expiresOn":"%s"}
                """.formatted(today, today.plusYears(1)));
        assertThat(lot.statusCode()).withFailMessage(lot.body()).isEqualTo(201);

        var report = call("POST", "/laboratories/" + lab.id() + "/inventory/reports", token, "{\"format\":\"CSV\"}");
        assertThat(report.statusCode()).withFailMessage(report.body()).isEqualTo(201);
        assertThat(report.headers().firstValue("location")).hasValueSatisfying(location ->
                assertThat(location).contains("/api/v1/reports/"));
        assertThat(report.body()).contains("\"reportType\":\"INVENTORY\"");
        var content = call("GET", "/reports/" + JsonPath.read(report.body(), "$.id") + "/content", token, null);
        assertThat(content.body()).contains("\"PA-001\",\"Paracetamol API\",\"kg\",\"20\",\"0\",\"100\",\"LOW\"",
                "\"Quimica Andina\",\"LOT-INV-1\",\"100\",\"100\"", "\"QUARANTINED\",\"VALID\"");

        assertThat(call("POST", "/laboratories/" + lab.id() + "/inventory/reports", token,
                "{\"environmentId\":" + lab.environment() + ",\"format\":\"PDF\"}").statusCode()).isEqualTo(201);
        assertThat(call("POST", "/laboratories/" + lab.id() + "/inventory/reports", token, "{}").statusCode()).isEqualTo(400);
        assertThat(call("POST", "/laboratories/" + lab.id() + "/inventory/reports", token,
                "{\"environmentId\":999999999,\"format\":\"CSV\"}").statusCode()).isEqualTo(400);
        var auditor = TestStaff.register(this::call, lab.id(), token, "Inventory auditor", "AUDITOR");
        assertThat(call("POST", "/laboratories/" + lab.id() + "/inventory/reports", auditor.token(), "{\"format\":\"CSV\"}")
                .statusCode()).isEqualTo(201);
        var foreign = laboratory();
        assertThat(call("POST", "/laboratories/" + lab.id() + "/inventory/reports", foreign.manager().token(),
                "{\"format\":\"CSV\"}").statusCode()).isEqualTo(403);
    }

    @Test
    void theBatchReportShowsTheContainerAndTheQualityDecision() throws Exception {
        var lab = laboratory();
        var token = lab.manager().token();
        var storage = environment(lab, "PT-" + suffix());
        assertThat(call("POST", "/laboratories/" + lab.id() + "/environments/" + storage + "/usage-assignments", token,
                "{\"usage\":\"PRODUCT_STORAGE\"}").statusCode()).isIn(200, 201);
        long monitor = id(call("POST", "/laboratories/" + lab.id() + "/devices/container-monitors", token,
                device("Finished goods shelf", "CNT-" + suffix())));
        assertThat(call("POST", "/laboratories/" + lab.id() + "/environments/" + storage + "/container-monitors", token,
                "{\"deviceId\":" + monitor + "}").statusCode()).isEqualTo(201);
        var products = lab.environmentPath() + "/products";
        long product = id(call("POST", products, token, """
                {"code":"PRD-%s","name":"Ibuprofen 400 mg","description":"Test product","specifications":"Tablet"}
                """.formatted(suffix())));
        var batches = products + "/" + product + "/batches";
        var today = LocalDate.now(LIMA);
        long batch = id(call("POST", batches, token, """
                {"batchNumber":"B-%s","quantity":5000,"unit":"units","startDate":"%s","notes":"Standard run"}
                """.formatted(suffix(), today)));
        assertThat(call("PUT", batches + "/" + batch + "/container-assignment", token, "{\"containerMonitorId\":" + monitor + "}")
                .statusCode()).isEqualTo(200);
        assertThat(call("POST", batches + "/" + batch + "/releases", token, """
                {"releaseDate":"%s","notes":"All quality controls passed"}
                """.formatted(today)).statusCode()).isEqualTo(201);

        var report = call("POST", "/batches/" + batch + "/reports", token, "{\"includeDeviations\":false,\"format\":\"PDF\"}");
        assertThat(report.statusCode()).withFailMessage(report.body()).isEqualTo(201);
        try (var document = Loader.loadPDF(binaryCall("/reports/" + JsonPath.read(report.body(), "$.id") + "/content", token))) {
            assertThat(new PDFTextStripper().getText(document)).contains("Equipment and staff",
                    "Stored in Finished goods shelf", "Released by user #" + lab.manager().id(), "SHA-256")
                    .doesNotContain("Telemetry: not included");
        }
        var csv = call("POST", "/batches/" + batch + "/reports", token, "{\"includeDeviations\":false,\"format\":\"CSV\"}");
        var content = call("GET", "/reports/" + JsonPath.read(csv.body(), "$.id") + "/content", token, null);
        assertThat(content.body()).contains("\"Container\",\"Finished goods shelf", "\"Release\",\"User #" + lab.manager().id());
    }

    /**
     * Readings of the container monitor: NORMAL, WARNING, NORMAL, NORMAL and CRITICAL over 50 minutes.
     */
    private Instant readTemperatureIncident(Lab lab) throws Exception {
        var token = lab.manager().token();
        assertThat(call("PUT", lab.container() + "/environmental-profile/thresholds", token, """
                {"thresholds":[{"metric":"TEMPERATURE","normalMin":15,"normalMax":25,"criticalMin":8,"criticalMax":30}]}
                """).statusCode()).isEqualTo(200);
        var start = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        var readings = lab.container() + "/telemetry-measurements";
        for (var reading : List.of(new Object[]{20.0, 0}, new Object[]{27.0, 600}, new Object[]{22.0, 1200},
                new Object[]{21.0, 2400}, new Object[]{33.0, 3000})) {
            var created = call("POST", readings, token, """
                    {"metric":"TEMPERATURE","value":%s,"measuredAt":"%s"}
                    """.formatted(reading[0], start.plusSeconds((Integer) reading[1])));
            assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        }
        return start;
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
                {"name":"Reporting laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
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
        var username = "reporting-" + UUID.randomUUID();
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
        assertThat(response.statusCode()).withFailMessage(response.body()).isBetween(200, 201);
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private byte[] binaryCall(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Authorization", "Bearer " + token).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
    }
}
