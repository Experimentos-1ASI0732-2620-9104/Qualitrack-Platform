package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.equipment.domain.repositories.MaintenanceRepository;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.*;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditLogRepository;
import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.*;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OnboardingIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired UserRepository users;
    @Autowired BatchRepository batches;
    @Autowired RawMaterialUsageRepository materialUsages;
    @Autowired com.iotech.qualitrack.platform.laboratory.domain.repositories.RawMaterialRepository legacyMaterials;
    @Autowired EquipmentRepository equipment;
    @Autowired MaintenanceRepository maintenance;
    @Autowired DeviationAlertRepository deviations;
    @Autowired AuditLogRepository audit;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final java.util.concurrent.atomic.AtomicLong ruc = new java.util.concurrent.atomic.AtomicLong(20100000000L);

    private record Account(Long id, String token) {}

    @Test
    void accountMustPayThenCreateLaboratoryAndCannotCreateTwice() throws Exception {
        var account = account();
        assertThat(call("GET", "/users/me/onboarding", account.token(), null).body())
                .contains("\"nextStep\":\"SUBSCRIPTION\"").contains("\"laboratoryId\":null");
        assertThat(call("GET", "/laboratories/1/equipments", account.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("POST", "/laboratories", account.token(), laboratory()).statusCode()).isEqualTo(403);
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        assertThat(call("GET", "/users/me/onboarding", account.token(), null).body())
                .contains("\"nextStep\":\"LABORATORY\"");
        var created = call("POST", "/laboratories", account.token(), laboratory());
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        Long laboratoryId = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
        assertThat(users.findById(account.id()).orElseThrow().getLaboratoryId()).isEqualTo(laboratoryId);
        assertThat(subscriptions.findActiveByUserId(account.id()).orElseThrow().getLaboratoryId()).isEqualTo(laboratoryId);
        assertThat(call("GET", "/users/me/onboarding", account.token(), null).body()).contains("\"nextStep\":\"READY\"");
        assertThat(call("POST", "/laboratories", account.token(), laboratory()).statusCode()).isEqualTo(409);
        assertThat(call("GET", "/laboratories/" + laboratoryId + "/equipments", account.token(), null).body()).isEqualTo("[]");
        var metrics = call("GET", "/laboratories/" + laboratoryId + "/kpi-dashboards", account.token(), null);
        assertThat(metrics.statusCode()).isEqualTo(200);
        assertThat(metrics.body()).contains("\"metrics\":[]").contains("\"overallHealthScore\":null");
    }

    @Test
    void expiredAndUnverifiedSubscriptionsDoNotUnlockTheApi() throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().minusDays(1));
        var state = call("GET", "/users/me/onboarding?success=true", account.token(), null);
        assertThat(state.body()).contains("\"nextStep\":\"SUBSCRIPTION\"");
        assertThat(call("GET", "/laboratories/1/equipments", account.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/users/me/onboarding", null, null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/users/me/onboarding", "invalid-token", null).statusCode()).isEqualTo(401);
    }

    @Test
    void accountCannotRegisterInAnotherLaboratoryOrSelfAssignAdministrator() throws Exception {
        var request = """
                {"username":"fixture-%1$s","email":"fixture-%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":1}
                """.formatted(UUID.randomUUID());
        assertThat(call("POST", "/authentication/sign-up", null, request).statusCode()).isEqualTo(400);
        request = request.replace("\"laboratoryId\":1", "\"laboratoryId\":null").replace("ROLE_QA_MANAGER", "ROLE_ADMIN");
        assertThat(call("POST", "/authentication/sign-up", null, request).statusCode()).isEqualTo(400);
    }

    @Test
    void completedAccountCannotReadOrWriteAnotherTenant() throws Exception {
        var first = account();
        var second = account();
        activateFixture(first, OffsetDateTime.now().plusDays(5));
        activateFixture(second, OffsetDateTime.now().plusDays(5));
        var firstLab = call("POST", "/laboratories", first.token(), laboratory());
        var secondLab = call("POST", "/laboratories", second.token(), laboratory());
        assertThat(firstLab.statusCode()).withFailMessage(firstLab.body()).isEqualTo(201);
        assertThat(secondLab.statusCode()).withFailMessage(secondLab.body()).isEqualTo(201);
        long foreignId = ((Number) JsonPath.read(secondLab.body(), "$.id")).longValue();
        assertThat(call("GET", "/laboratories/" + foreignId, first.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/laboratories/" + foreignId + "/equipments", first.token(), null).statusCode()).isEqualTo(403);
        long foreignSubscription = subscriptions.findActiveByUserId(second.id()).orElseThrow().getId();
        assertThat(call("GET", "/subscriptions/" + foreignSubscription + "/payments", first.token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void checkoutRejectsForgedIdentityAndWebhookRejectsInvalidSignature() throws Exception {
        var account = account();
        var result = call("POST", "/subscription-checkout-sessions", account.token(), """
                {"userId":999999,"planCode":"BASIC","billingCycle":"MONTHLY","successUrl":"http://localhost:4200/subscriptions/success",
                "cancelUrl":"http://localhost:4200/subscriptions/cancel"}
                """);
        assertThat(result.statusCode()).isEqualTo(403);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/stripe/webhooks"))
                .header("Content-Type", "application/json").header("Stripe-Signature", "invalid")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertThat(http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
    }

    @Test
    void generatedReportCanBeDownloadedAgainWithIdenticalBytes() throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        var created = call("POST", "/laboratories", account.token(), laboratory());
        long lab = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
        var response = call("POST", "/laboratories/" + lab + "/compliance-reports", account.token(), """
                {"startDate":"2026-08-01","endDate":"2026-09-01","format":"CSV","requestedBy":%d}
                """.formatted(account.id()));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);
        long report = ((Number) JsonPath.read(response.body(), "$.id")).longValue();
        var content = call("GET", "/reports/" + report + "/content", account.token(), null);
        assertThat(content.statusCode()).withFailMessage(content.body()).isEqualTo(200);
        assertThat(content.body()).contains("The laboratory has no environments.", "Whole calendar days");
        assertThat(call("GET", "/reports/" + report + "/content", account.token(), null).body()).isEqualTo(content.body());
        assertThat(content.headers().firstValue("content-disposition").orElseThrow()).contains("attachment");
        var other = account();
        activateFixture(other, OffsetDateTime.now().plusDays(5));
        call("POST", "/laboratories", other.token(), laboratory());
        assertThat(call("GET", "/reports/" + report + "/content", other.token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void batchPdfIncludesPersistedMaterialsAndRetainsTenantProtectionAndDownloadHistory() throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        var created = call("POST", "/laboratories", account.token(), laboratory());
        long lab = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
        var batch = batches.save(new Batch(null, lab, 1L, 51L, "Fixture product", "FIXTURE-PDF-" + UUID.randomUUID(),
                150.0, "units", BatchStatus.RELEASED, "2026-09-01", "2026-09-03", "DEMO: isolated test record"));
        materialUsages.save(new RawMaterialUsage(null, batch.getId(), 71L, "Fixture material from persistence",
                75.0, "g", "2026-09-01"));
        var body = """
                {"includeDeviations":true,"format":"PDF"}
                """;
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/batches/" + batch.getId() + "/reports"))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + account.token())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var requested = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(requested.statusCode()).withFailMessage(requested.body()).isEqualTo(201);
        long report = ((Number) JsonPath.read(requested.body(), "$.id")).longValue();
        var generated = binaryCall("GET", "/reports/" + report + "/content", account.token(), null);
        assertThat(generated.statusCode()).isEqualTo(200);
        assertThat(generated.headers().firstValue("content-type").orElseThrow()).contains("application/pdf");
        try (var pdf = Loader.loadPDF(generated.body())) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("Material traceability", "Fixture material from persistence",
                    "75", "0 recorded deviations", "DEMO: isolated test record");
        }
        assertThat(binaryCall("GET", "/reports/" + report + "/content", account.token(), null).body()).isEqualTo(generated.body());
        var other = account();
        activateFixture(other, OffsetDateTime.now().plusDays(5));
        call("POST", "/laboratories", other.token(), laboratory());
        assertThat(call("POST", "/batches/" + batch.getId() + "/reports", other.token(), body).statusCode()).isEqualTo(403);
        assertThat(call("POST", "/batches/999999999/reports", account.token(), body).statusCode()).isIn(403, 404);
    }

    @Test
    void periodReportsIncludeOnlySelectedRecordsAndKeepAuthorizationAndBinaryHistory() throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        var created = call("POST", "/laboratories", account.token(), laboratory());
        long lab = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
        var environment = call("POST", "/laboratories/" + lab + "/environments", account.token(),
                "{\"code\":\"ENV-01\",\"name\":\"Period environment\"}");
        assertThat(environment.statusCode()).withFailMessage(environment.body()).isEqualTo(201);
        long environmentId = ((Number) JsonPath.read(environment.body(), "$.id")).longValue();
        var device = equipment.save(new Equipment(null, lab, environmentId, "DEMO period fixture", new EquipmentType("Refrigerator"),
                "Model", "SN-" + UUID.randomUUID(), EquipmentStatus.OPERATIONAL, null, null, null));
        for (String day : java.util.List.of("2026-08-31", "2026-09-01", "2026-09-05", "2026-09-06")) {
            deviations.save(new DeviationAlert(null, device.getId(), null, "PARAM-" + day, 9.3, 8.0, "C", day + "T12:00:00",
                    AlertSeverity.CRITICAL, AlertStatus.UNRESOLVED, null, null, null));
            maintenance.save(new MaintenanceRecord(null, device.getId(), null, java.time.LocalDate.parse(day),
                    "Fixture technician", null, "MAINT-" + day, MaintenanceType.CALIBRATION));
            audit.save(new AuditLogEntry(null, AuditAction.UPDATE, "EQUIPMENT", device.getId(), account.id(), day + "T12:00:00", "LOG-" + day));
        }
        String body = """
                {"startDate":"2026-09-01","endDate":"2026-09-05","format":"PDF","requestedBy":%d}
                """.formatted(account.id());
        for (String path : java.util.List.of("/laboratories/" + lab + "/compliance-reports", "/laboratories/" + lab + "/environments/" + environmentId + "/equipments/" + device.getId() + "/log-reports")) {
            var requested = call("POST", path, account.token(), body);
            assertThat(requested.statusCode()).withFailMessage(requested.body()).isEqualTo(201);
            long reportId = ((Number) JsonPath.read(requested.body(), "$.id")).longValue();
            var generated = binaryCall("GET", "/reports/" + reportId + "/content", account.token(), null);
            assertThat(generated.statusCode()).isEqualTo(200);
            assertThat(generated.headers().firstValue("content-type").orElseThrow()).contains("application/pdf");
            try (var pdf = Loader.loadPDF(generated.body())) {
                String text = new PDFTextStripper().getText(pdf);
                assertThat(text).doesNotContain("2026-08-31", "2026-09-06");
                if (path.contains("compliance")) assertThat(text).contains("Environmental report");
                else assertThat(text).contains("MAINT-2026-09-01", "MAINT-2026-09-05", "LOG-2026-09-01", "LOG-2026-09-05");
            }
            assertThat(binaryCall("GET", "/reports/" + reportId + "/content", account.token(), null).body()).isEqualTo(generated.body());
            var csvCreated = call("POST", path, account.token(), body.replace("PDF", "CSV"));
            assertThat(csvCreated.statusCode()).isEqualTo(201);
            long csvId = ((Number) JsonPath.read(csvCreated.body(), "$.id")).longValue();
            var csv = call("GET", "/reports/" + csvId + "/content", account.token(), null);
            assertThat(csv.body()).contains("2026-09-01", "2026-09-05").doesNotContain("2026-08-31", "2026-09-06");
            assertThat(call("POST", path, account.token(), body.replace("2026-09-01", "2026-09-07")).statusCode()).isEqualTo(400);
            assertThat(call("POST", path, null, body).statusCode()).isEqualTo(401);
        }
        var other = account();
        activateFixture(other, OffsetDateTime.now().plusDays(5));
        call("POST", "/laboratories", other.token(), laboratory());
        String otherBody = body.replace("\"requestedBy\":" + account.id(), "\"requestedBy\":" + other.id());
        assertThat(call("POST", "/laboratories/" + lab + "/compliance-reports", other.token(), otherBody).statusCode()).isEqualTo(403);
        assertThat(call("POST", "/laboratories/" + lab + "/environments/" + environmentId + "/equipments/" + device.getId() + "/log-reports", other.token(), otherBody).statusCode()).isEqualTo(403);
    }

    private HttpResponse<byte[]> binaryCall(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void concurrentLaboratoryRequestsAssociateOnlyOneLaboratory() throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of(
                    () -> call("POST", "/laboratories", account.token(), laboratory()).statusCode(),
                    () -> call("POST", "/laboratories", account.token(), laboratory()).statusCode()));
            assertThat(java.util.List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void materialConsumptionRejectsOverdraftAndPersistsStockAndHistory() throws Exception {
        var fixture = stockFixture("kg");
        var denied = consume(fixture, "200", "kg");
        assertThat(denied.statusCode()).withFailMessage(denied.body()).isEqualTo(409);
        assertThat(denied.body()).contains("Insufficient stock");
        assertThat(stock(fixture)).isEqualTo(100);
        assertThat(materialUsages.findAllByBatchId(fixture.batch())).isEmpty();
        var accepted = consume(fixture, "50", "kg");
        assertThat(accepted.statusCode()).withFailMessage(accepted.body()).isEqualTo(201);
        assertThat(stock(fixture)).isEqualTo(50);
        assertThat(((Number) JsonPath.read(accepted.body(), "$.stockBefore")).doubleValue()).isEqualTo(100);
        assertThat(((Number) JsonPath.read(accepted.body(), "$.stockAfter")).doubleValue()).isEqualTo(50);
        var history = call("GET", fixture.environmentPath() + "/raw-materials/" + fixture.material() + "/movements", fixture.account().token(), null);
        assertThat(history.statusCode()).withFailMessage(history.body()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(history.body(), "$[0].productBatchId")).longValue()).isEqualTo(fixture.batch());
        assertThat(((Number) JsonPath.read(history.body(), "$[0].amount")).doubleValue()).isEqualTo(-50);
        assertThat(consume(fixture, "50", "kg").statusCode()).isEqualTo(201);
        assertThat(stock(fixture)).isZero();
        assertThat(consume(fixture, "0.001", "kg").statusCode()).isEqualTo(409);
    }

    @Test
    void materialConsumptionValidatesUnitsDecimalsAndPositiveAmounts() throws Exception {
        var fixture = stockFixture("L");
        assertThat(consume(fixture, "50", "kg").statusCode()).isEqualTo(400);
        assertThat(consume(fixture, "0", "L").statusCode()).isEqualTo(400);
        assertThat(consume(fixture, "-1", "L").statusCode()).isEqualTo(400);
        assertThat(consume(fixture, "0.0001", "L").statusCode()).isEqualTo(400);
        assertThat(consume(fixture, "50", "").statusCode()).isEqualTo(400);
        assertThat(stock(fixture)).isEqualTo(100);
        assertThat(consume(fixture, "0.125", "Liters").statusCode()).isEqualTo(201);
        assertThat(stock(fixture)).isEqualTo(99.875);
        var units = stockFixture("units");
        assertThat(consume(units, "0.5", "units").statusCode()).isEqualTo(400);
        assertThat(stock(units)).isEqualTo(100);
    }

    @Test
    void concurrentConsumersCannotOverdrawInventory() throws Exception {
        var fixture = stockFixture("kg");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> { gate.await(); return consume(fixture, "75", "kg").statusCode(); });
            var second = executor.submit(() -> { gate.await(); return consume(fixture, "75", "kg").statusCode(); });
            gate.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(stock(fixture)).isEqualTo(25);
        assertThat(materialUsages.findAllByBatchId(fixture.batch())).hasSize(1);
    }

    @Test
    void materialHistoryAndConsumptionRejectOtherLaboratories() throws Exception {
        var owner = stockFixture("kg");
        var other = stockFixture("kg");
        var crossed = new StockFixture(other.account(), other.lab(), other.environment(), other.product(), other.batch(), owner.material(), owner.receipt());
        assertThat(consume(crossed, "10", "kg").statusCode()).isEqualTo(404);
        assertThat(call("GET", owner.environmentPath() + "/raw-materials/" + owner.material() + "/movements", other.account().token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", owner.environmentPath() + "/raw-materials/" + owner.material() + "/movements", null, null).statusCode()).isEqualTo(401);
        assertThat(stock(owner)).isEqualTo(100);
    }

    @Test
    void historicalUsagesDoNotInventStockBalances() throws Exception {
        var fixture = stockFixture("kg");
        var legacy = legacyMaterials.save(new com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.RawMaterial(null,
                fixture.lab(), "Legacy material", "OLD-" + UUID.randomUUID(), "Supplier", "SUP-OLD", "2028-01-01",
                new java.math.BigDecimal("50"), "kg", java.math.BigDecimal.TEN));
        materialUsages.save(new RawMaterialUsage(null, fixture.batch(), legacy.getId(), "Legacy material",
                200.0, "kg", "2026-09-01"));
        var history = call("GET", "/laboratories/" + fixture.lab() + "/raw-materials/" + legacy.getId() + "/usages", fixture.account().token(), null);
        assertThat(history.body()).contains("\"stockBefore\":null", "\"stockAfter\":null");
        assertThat(stock(fixture)).isEqualTo(100);
    }

    private record StockFixture(Account account, long lab, long environment, long product, long batch, long material, long receipt) {
        String environmentPath() { return "/laboratories/" + lab + "/environments/" + environment; }
    }

    private StockFixture stockFixture(String unit) throws Exception {
        var account = account();
        activateFixture(account, OffsetDateTime.now().plusDays(5));
        var laboratory = call("POST", "/laboratories", account.token(), laboratory());
        long lab = ((Number) JsonPath.read(laboratory.body(), "$.id")).longValue();
        var environment = call("POST", "/laboratories/" + lab + "/environments", account.token(),
                "{\"code\":\"ENV-01\",\"name\":\"Stock environment\"}");
        assertThat(environment.statusCode()).withFailMessage(environment.body()).isEqualTo(201);
        long environmentId = ((Number) JsonPath.read(environment.body(), "$.id")).longValue();
        var path = "/laboratories/" + lab + "/environments/" + environmentId;
        var material = call("POST", path + "/raw-materials", account.token(), """
                {"name":"Stock fixture","code":"RM-%s","unit":"%s","minimumStock":10}
                """.formatted(UUID.randomUUID(), unit));
        assertThat(material.statusCode()).withFailMessage(material.body()).isEqualTo(201);
        long materialId = ((Number) JsonPath.read(material.body(), "$.id")).longValue();
        var receipt = call("POST", path + "/raw-materials/" + materialId + "/batches", account.token(), """
                {"supplier":"Test supplier","batchNumber":"SUP-1","unit":"%s","amount":100,
                 "receivedOn":"%s","expiresOn":"%s"}
                """.formatted(unit, java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().plusYears(1)));
        assertThat(receipt.statusCode()).withFailMessage(receipt.body()).isEqualTo(201);
        long receiptId = ((Number) JsonPath.read(receipt.body(), "$.id")).longValue();
        var review = call("POST", path + "/raw-materials/" + materialId + "/batches/" + receiptId + "/reviews", account.token(),
                "{\"status\":\"RELEASED\",\"reason\":\"Certificate and quantity reviewed\"}");
        assertThat(review.statusCode()).withFailMessage(review.body()).isBetween(200, 201);
        var product = call("POST", path + "/products", account.token(), """
                {"name":"Stock test product","code":"P-%s","description":"Test","specifications":"Test only"}
                """.formatted(UUID.randomUUID()));
        assertThat(product.statusCode()).withFailMessage(product.body()).isEqualTo(201);
        long productId = ((Number) JsonPath.read(product.body(), "$.id")).longValue();
        var batch = call("POST", path + "/products/" + productId + "/batches", account.token(), """
                {"batchNumber":"STOCK-%s","quantity":10,"unit":"g","startDate":"2026-09-08","notes":""}
                """.formatted(UUID.randomUUID()));
        assertThat(batch.statusCode()).withFailMessage(batch.body()).isEqualTo(201);
        assertThat(JsonPath.<String>read(batch.body(), "$.unit")).isEqualTo("g");
        return new StockFixture(account, lab, environmentId, productId, ((Number) JsonPath.read(batch.body(), "$.id")).longValue(), materialId, receiptId);
    }

    private HttpResponse<String> consume(StockFixture fixture, String quantity, String unit) throws Exception {
        return call("POST", fixture.environmentPath() + "/products/" + fixture.product() + "/batches/" + fixture.batch()
                + "/raw-material-usages", fixture.account().token(),
                "{\"rawMaterialBatchId\":" + fixture.receipt()
                + ",\"amountUsed\":" + quantity + ",\"unit\":\"" + unit + "\",\"operationId\":\"" + UUID.randomUUID() + "\"}");
    }

    private double stock(StockFixture fixture) throws Exception {
        var response = call("GET", fixture.environmentPath() + "/raw-materials/" + fixture.material() + "/stock", fixture.account().token(), null);
        return ((Number) JsonPath.read(response.body(), "$.usableStock")).doubleValue();
    }

    private Account account() throws Exception {
        var username = "fixture-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%s","email":"%s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username, username));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        assertThat(registration.body()).doesNotContain("\"token\"");
        var response = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), JsonPath.read(response.body(), "$.token"));
    }

    private void activateFixture(Account account, OffsetDateTime end) {
        var unique = UUID.randomUUID().toString();
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(account.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
    }

    private String laboratory() {
        return """
                {"name":"Fixture laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(UUID.randomUUID(), ruc.incrementAndGet());
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
