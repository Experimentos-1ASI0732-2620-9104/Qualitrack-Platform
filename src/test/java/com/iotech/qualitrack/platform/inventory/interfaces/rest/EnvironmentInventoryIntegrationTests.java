package com.iotech.qualitrack.platform.inventory.interfaces.rest;

import com.iotech.qualitrack.platform.testing.LimaDates;
import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers TS21-TS28 (raw materials per environment) and TS79 (product batches that used a raw material)
 * through the public REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvironmentInventoryIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired IamContextFacade iam;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20400000000L);

    private record Account(Long id, String token) {}

    private record Warehouse(Account manager, long lab, long environment) {
        String rawMaterials() { return "/laboratories/" + lab + "/environments/" + environment + "/raw-materials"; }
    }

    @Test
    void qualityManagerRegistersRawMaterialsAndReviewsTheirLots() throws Exception {
        var warehouse = warehouse();
        var token = warehouse.manager().token();

        assertThat(call("GET", warehouse.rawMaterials(), token, null).body()).isEqualTo("[]");

        var created = registerMaterial(warehouse, "kg", 50);
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long material = id(created);
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/api/v1" + warehouse.rawMaterials() + "/" + material));
        assertThat(created.body()).contains("\"environmentId\":" + warehouse.environment()).contains("\"stockStatus\":\"LOW\"");

        var duplicate = call("POST", warehouse.rawMaterials(), token, materialBody(
                JsonPath.read(created.body(), "$.code"), "kg", 1));
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(registerMaterial(warehouse, "barrels", 1).statusCode()).isEqualTo(400);

        var lot = receive(warehouse, material, "LOT-A", "kg", LimaDates.today().plusYears(1), token);
        assertThat(lot.statusCode()).withFailMessage(lot.body()).isEqualTo(201);
        long lotId = id(lot);
        assertThat(lot.body()).contains("\"status\":\"QUARANTINED\"").contains("\"expirationStatus\":\"VALID\"");
        assertThat(receive(warehouse, material, "LOT-A", "kg", LimaDates.today().plusYears(1), token).statusCode()).isEqualTo(409);

        var stockBefore = call("GET", warehouse.rawMaterials() + "/" + material + "/stock", token, null);
        assertThat(((Number) JsonPath.read(stockBefore.body(), "$.usableStock")).doubleValue()).isZero();
        assertThat(((Number) JsonPath.read(stockBefore.body(), "$.physicalStock")).doubleValue()).isEqualTo(100);
        assertThat(call("GET", warehouse.rawMaterials() + "/" + material + "/batches?usable=true", token, null).body())
                .isEqualTo("[]");

        var review = call("POST", warehouse.rawMaterials() + "/" + material + "/batches/" + lotId + "/reviews", token,
                "{\"status\":\"RELEASED\",\"reason\":\"Certificate of analysis verified\"}");
        assertThat(review.statusCode()).withFailMessage(review.body()).isEqualTo(201);
        assertThat(review.body()).contains("\"previousStatus\":\"QUARANTINED\"").contains("\"status\":\"RELEASED\"")
                .contains("\"reviewedBy\":" + warehouse.manager().id());
        assertThat(call("POST", warehouse.rawMaterials() + "/" + material + "/batches/" + lotId + "/reviews", token,
                "{\"status\":\"RELEASED\",\"reason\":\"Again\"}").statusCode()).isEqualTo(409);

        var stockAfter = call("GET", warehouse.rawMaterials() + "/" + material + "/stock", token, null);
        assertThat(((Number) JsonPath.read(stockAfter.body(), "$.usableStock")).doubleValue()).isEqualTo(100);
        assertThat(stockAfter.body()).contains("\"stockStatus\":\"SUFFICIENT\"");
        assertThat(JsonPath.<List<Integer>>read(call("GET", warehouse.rawMaterials() + "/" + material + "/batches?usable=true",
                token, null).body(), "$[*].id")).containsExactly((int) lotId);
        assertThat(call("GET", warehouse.rawMaterials() + "/" + material + "/batches/" + lotId, token, null).statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(call("GET", warehouse.rawMaterials() + "/" + material + "/movements", token, null).body(),
                "$[*].type")).containsExactly("REVIEW", "RECEIPT");
    }

    @Test
    void lowStockAndNearExpiryFiltersIdentifyMaterialsAndLots() throws Exception {
        var warehouse = warehouse();
        var token = warehouse.manager().token();
        long low = id(registerMaterial(warehouse, "kg", 500));
        long sufficient = id(registerMaterial(warehouse, "kg", 0));
        long soon = id(receive(warehouse, low, "SOON", "kg", LimaDates.today().plusDays(10), token));
        id(receive(warehouse, sufficient, "LATER", "kg", LimaDates.today().plusDays(200), token));

        var lowStock = call("GET", warehouse.rawMaterials() + "?stockStatus=LOW", token, null);
        assertThat(JsonPath.<List<Integer>>read(lowStock.body(), "$[*].id")).containsExactly((int) low);
        assertThat(call("GET", warehouse.rawMaterials() + "?stockStatus=EMPTY", token, null).statusCode()).isEqualTo(400);

        var batches = "/laboratories/" + warehouse.lab() + "/environments/" + warehouse.environment() + "/raw-material-batches";
        var nearExpiry = call("GET", batches + "?expirationStatus=NEAR_EXPIRY", token, null);
        assertThat(nearExpiry.statusCode()).withFailMessage(nearExpiry.body()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(nearExpiry.body(), "$[*].id")).containsExactly((int) soon);
        assertThat(JsonPath.<List<Integer>>read(call("GET", batches + "?expirationStatus=NEAR_EXPIRY&withinDays=365", token, null).body(),
                "$[*].id")).hasSize(2);
        assertThat(JsonPath.<List<Integer>>read(call("GET", batches, token, null).body(), "$[*].id")).hasSize(2);
        assertThat(call("GET", batches + "?expirationStatus=SOMEDAY", token, null).statusCode()).isEqualTo(400);
        assertThat(call("GET", batches + "?expirationStatus=NEAR_EXPIRY&withinDays=-1", token, null).statusCode()).isEqualTo(400);
    }

    @Test
    void rawMaterialsAreScopedToTheirEnvironmentAndLaboratory() throws Exception {
        var warehouse = warehouse();
        var token = warehouse.manager().token();
        long material = id(registerMaterial(warehouse, "L", 1));
        var other = environment(warehouse.lab(), token, "WH-OTHER");
        var otherMaterials = "/laboratories/" + warehouse.lab() + "/environments/" + other + "/raw-materials";

        assertThat(call("GET", otherMaterials + "/" + material, token, null).statusCode()).isEqualTo(404);
        assertThat(call("GET", otherMaterials + "/" + material + "/batches", token, null).statusCode()).isEqualTo(404);
        assertThat(call("GET", otherMaterials + "/" + material + "/usages", token, null).statusCode()).isEqualTo(404);
        assertThat(call("GET", otherMaterials, token, null).body()).isEqualTo("[]");

        var foreign = warehouse();
        assertThat(call("GET", warehouse.rawMaterials(), foreign.manager().token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", foreign.rawMaterials() + "/" + material, foreign.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void operatorReceivesLotsButCannotChangeCatalogOrReview() throws Exception {
        var warehouse = warehouse();
        long material = id(registerMaterial(warehouse, "units", 5));
        var operator = TestStaff.register(this::call, warehouse.lab(), warehouse.manager().token(), "Warehouse operator", "OPERATOR");

        assertThat(registerMaterialAs(warehouse, operator.token()).statusCode()).isEqualTo(403);
        var lot = receive(warehouse, material, "OP-1", "units", LimaDates.today().plusMonths(6), operator.token());
        assertThat(lot.statusCode()).withFailMessage(lot.body()).isEqualTo(201);
        assertThat(call("POST", warehouse.rawMaterials() + "/" + material + "/batches/" + id(lot) + "/reviews", operator.token(),
                "{\"status\":\"RELEASED\",\"reason\":\"Not allowed\"}").statusCode()).isEqualTo(403);
    }

    @Test
    void usagesIdentifyTheProductBatchesThatConsumedARawMaterial() throws Exception {
        var warehouse = warehouse();
        var token = warehouse.manager().token();
        long material = id(registerMaterial(warehouse, "kg", 1));
        long lot = id(receive(warehouse, material, "USE-1", "kg", LimaDates.today().plusYears(1), token));
        call("POST", warehouse.rawMaterials() + "/" + material + "/batches/" + lot + "/reviews", token,
                "{\"status\":\"RELEASED\",\"reason\":\"Reviewed\"}");
        var usages = warehouse.rawMaterials() + "/" + material + "/usages";
        assertThat(call("GET", usages, token, null).body()).isEqualTo("[]");

        var product = call("POST", "/laboratories/" + warehouse.lab() + "/environments/" + warehouse.environment() + "/products", token, """
                {"name":"Usage product","code":"P-%s","description":"Test","specifications":"Test only"}
                """.formatted(UUID.randomUUID()));
        assertThat(product.statusCode()).withFailMessage(product.body()).isEqualTo(201);
        long productId = id(product);
        var batch = call("POST", "/laboratories/" + warehouse.lab() + "/environments/" + warehouse.environment() + "/products/" + productId + "/batches", token, """
                {"batchNumber":"USE-%s","quantity":10,"unit":"g","startDate":"%s","notes":""}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), LocalDate.now()));
        assertThat(batch.statusCode()).withFailMessage(batch.body()).isEqualTo(201);
        long batchId = id(batch);
        var consumption = call("POST", "/laboratories/" + warehouse.lab() + "/environments/" + warehouse.environment() + "/products/"
                + productId + "/batches/" + batchId + "/raw-material-usages", token,
                "{\"rawMaterialBatchId\":" + lot + ",\"amountUsed\":30,\"unit\":\"kg\",\"operationId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(consumption.statusCode()).withFailMessage(consumption.body()).isEqualTo(201);

        var used = call("GET", usages, token, null);
        assertThat(used.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(used.body(), "$[*].batchId")).containsExactly((int) batchId);
        assertThat(JsonPath.<List<Integer>>read(used.body(), "$[*].inventoryReceiptId")).containsExactly((int) lot);
    }

    private HttpResponse<String> registerMaterial(Warehouse warehouse, String unit, int minimumStock) throws Exception {
        return call("POST", warehouse.rawMaterials(), warehouse.manager().token(),
                materialBody("RM-" + UUID.randomUUID().toString().substring(0, 8), unit, minimumStock));
    }

    private HttpResponse<String> registerMaterialAs(Warehouse warehouse, String token) throws Exception {
        return call("POST", warehouse.rawMaterials(), token, materialBody("RM-" + UUID.randomUUID().toString().substring(0, 8), "kg", 1));
    }

    private static String materialBody(String code, String unit, int minimumStock) {
        return "{\"code\":\"%s\",\"name\":\"Active ingredient\",\"unit\":\"%s\",\"minimumStock\":%d}".formatted(code, unit, minimumStock);
    }

    private HttpResponse<String> receive(Warehouse warehouse, long material, String lot, String unit, LocalDate expiresOn,
                                         String token) throws Exception {
        return call("POST", warehouse.rawMaterials() + "/" + material + "/batches", token, """
                {"supplier":"Supplier SAC","batchNumber":"%s","unit":"%s","amount":100,"receivedOn":"%s","expiresOn":"%s"}
                """.formatted(lot, unit, LimaDates.today().minusDays(1), expiresOn));
    }

    private Warehouse warehouse() throws Exception {
        var manager = account("ROLE_QA_MANAGER");
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Inventory laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        return new Warehouse(manager, lab, environment(lab, manager.token(), "WH-RM-01"));
    }

    private long environment(long lab, String token, String code) throws Exception {
        var created = call("POST", "/laboratories/" + lab + "/environments", token,
                "{\"code\":\"" + code + "\",\"name\":\"Raw material warehouse\"}");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private Account account(String role) throws Exception {
        var username = "inventory-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["%2$s"],"laboratoryId":null}
                """.formatted(username, role));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(id(response), JsonPath.read(response.body(), "$.token"));
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
