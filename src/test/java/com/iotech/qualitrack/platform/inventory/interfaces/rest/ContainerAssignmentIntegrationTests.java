package com.iotech.qualitrack.platform.inventory.interfaces.rest;

import com.iotech.qualitrack.platform.testing.LimaDates;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the storage of raw material lots and product batches in monitored containers (US43, US44, US78, US79,
 * TS29, TS30, TS68, TS69).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContainerAssignmentIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20980000000L);

    private record Account(Long id, String token) { }

    /**
     * Laboratory with a raw material warehouse, a production area and a product storage area, each with a container
     * monitor, except production.
     */
    private record Plant(Account manager, long lab, long rawStore, long production, long productStore,
                         long rawContainer, long productContainer) {
        String environment(long environmentId) { return "/laboratories/" + lab + "/environments/" + environmentId; }
    }

    @Test
    void rawMaterialLotsAreStoredInContainersOfTheirRawMaterialStorageEnvironment() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long material = id(call("POST", plant.environment(plant.rawStore()) + "/raw-materials", token, material()));
        var lots = plant.environment(plant.rawStore()) + "/raw-materials/" + material + "/batches";
        long lot = id(call("POST", lots, token, lot("LOT-" + suffix())));
        var assignment = lots + "/" + lot + "/container-assignment";

        var none = call("GET", assignment, token, null);
        assertThat(none.statusCode()).isEqualTo(404);
        assertThat(none.body()).contains("\"code\"");

        var operator = TestStaff.register(this::call, plant.lab(), token, "Warehouse operator", "OPERATOR");
        var stored = call("PUT", assignment, operator.token(), container(plant.rawContainer()));
        assertThat(stored.statusCode()).withFailMessage(stored.body()).isEqualTo(200);
        assertThat(stored.body()).contains("\"containerMonitorId\":" + plant.rawContainer(), "\"containerName\":\"Raw shelf monitor\"",
                "\"environmentId\":" + plant.rawStore(), "\"assignedBy\":" + operator.userId());
        assertThat(call("GET", assignment, token, null).body()).contains("\"containerMonitorId\":" + plant.rawContainer());
        assertThat(call("GET", lots, token, null).body()).contains("\"containerMonitorId\":" + plant.rawContainer());

        assertThat(call("PUT", assignment, token, container(plant.rawContainer())).statusCode()).isEqualTo(200);
        var movements = call("GET", plant.environment(plant.rawStore()) + "/raw-materials/" + material + "/movements", token, null);
        assertThat(JsonPath.<List<String>>read(movements.body(), "$[?(@.type == 'STORAGE')].type")).hasSize(1);

        assertThat(call("PUT", assignment, token, container(plant.productContainer())).statusCode()).isEqualTo(409);
        assertThat(call("PUT", assignment, token, container(999999)).statusCode()).isEqualTo(409);
        assertThat(call("PUT", assignment, token, "{}").statusCode()).isEqualTo(400);
        var auditor = TestStaff.register(this::call, plant.lab(), token, "Auditor", "AUDITOR");
        assertThat(call("PUT", assignment, auditor.token(), container(plant.rawContainer())).statusCode()).isEqualTo(403);
        assertThat(call("GET", assignment, auditor.token(), null).statusCode()).isEqualTo(200);

        long secondContainer = containerIn(plant, plant.rawStore(), "Second raw shelf");
        var maintenance = call("POST", plant.environment(plant.rawStore()) + "/equipments/" + secondContainer + "/status-changes",
                token, "{\"status\":\"MAINTENANCE\",\"reason\":\"Sensor calibration\"}");
        assertThat(maintenance.statusCode()).withFailMessage(maintenance.body()).isEqualTo(201);
        var unavailable = call("PUT", assignment, token, container(secondContainer));
        assertThat(unavailable.statusCode()).isEqualTo(409);
        assertThat(unavailable.body()).contains("MAINTENANCE");

        long productionMaterial = id(call("POST", plant.environment(plant.production()) + "/raw-materials", token, material()));
        var productionLots = plant.environment(plant.production()) + "/raw-materials/" + productionMaterial + "/batches";
        long productionLot = id(call("POST", productionLots, token, lot("PRD-" + suffix())));
        assertThat(call("PUT", productionLots + "/" + productionLot + "/container-assignment", token,
                container(plant.rawContainer())).statusCode()).isEqualTo(409);

        var foreign = plant();
        assertThat(call("GET", assignment, foreign.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void productBatchesAreStoredInContainersOfProductStorageEnvironments() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var products = plant.environment(plant.production()) + "/products";
        long product = id(call("POST", products, token, """
                {"code":"PRD-%s","name":"Ibuprofen 400 mg","description":"Test product","specifications":"Tablet"}
                """.formatted(suffix())));
        var batches = products + "/" + product + "/batches";
        long batch = id(call("POST", batches, token, """
                {"batchNumber":"B-%s","quantity":5000,"unit":"units","startDate":"%s","notes":"Standard run"}
                """.formatted(suffix(), LocalDate.now())));
        var assignment = batches + "/" + batch + "/container-assignment";

        assertThat(call("GET", assignment, token, null).statusCode()).isEqualTo(404);
        var stored = call("PUT", assignment, token, container(plant.productContainer()));
        assertThat(stored.statusCode()).withFailMessage(stored.body()).isEqualTo(200);
        assertThat(stored.body()).contains("\"containerMonitorId\":" + plant.productContainer(),
                "\"environmentId\":" + plant.productStore(), "\"containerName\":\"Product rack monitor\"");
        assertThat(call("GET", assignment, token, null).body()).contains("\"environmentId\":" + plant.productStore());
        assertThat(call("GET", batches + "/" + batch, token, null).body()).contains("\"containerMonitorId\":" + plant.productContainer());
        assertThat(call("GET", batches + "/" + batch + "/traceability", token, null).body())
                .contains("\"container\":{").contains("\"containerName\":\"Product rack monitor\"");

        assertThat(call("PUT", assignment, token, container(plant.rawContainer())).statusCode()).isEqualTo(409);
        assertThat(call("PUT", assignment, token, container(999999)).statusCode()).isEqualTo(409);
        assertThat(call("PUT", batches + "/999999/container-assignment", token, container(plant.productContainer())).statusCode())
                .isIn(403, 404);
        var foreign = plant();
        assertThat(call("PUT", assignment, foreign.manager().token(), container(foreign.productContainer())).statusCode())
                .isEqualTo(403);
    }

    private Plant plant() throws Exception {
        var manager = account();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Container laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        long rawStore = environment(manager, lab, "RAW", "RAW_MATERIAL_STORAGE");
        long production = environment(manager, lab, "PRD", "PRODUCTION");
        long productStore = environment(manager, lab, "FG", "PRODUCT_STORAGE");
        var plant = new Plant(manager, lab, rawStore, production, productStore, 0, 0);
        long rawContainer = containerIn(plant, rawStore, "Raw shelf monitor");
        long productContainer = containerIn(plant, productStore, "Product rack monitor");
        return new Plant(manager, lab, rawStore, production, productStore, rawContainer, productContainer);
    }

    private long environment(Account manager, long lab, String prefix, String usage) throws Exception {
        var created = call("POST", "/laboratories/" + lab + "/environments", manager.token(),
                "{\"code\":\"" + prefix + "-" + suffix() + "\",\"name\":\"Environment " + prefix + "\"}");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long environment = id(created);
        var assigned = call("POST", "/laboratories/" + lab + "/environments/" + environment + "/usage-assignments", manager.token(),
                "{\"usage\":\"" + usage + "\"}");
        assertThat(assigned.statusCode()).withFailMessage(assigned.body()).isEqualTo(201);
        return environment;
    }

    private long containerIn(Plant plant, long environment, String name) throws Exception {
        var token = plant.manager().token();
        var registered = call("POST", "/laboratories/" + plant.lab() + "/devices/container-monitors", token, """
                {"name":"%s","sensorExternalId":"CNT-%s","serialNumber":"MAC-%s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(name, suffix(), suffix()));
        assertThat(registered.statusCode()).withFailMessage(registered.body()).isEqualTo(201);
        long container = id(registered);
        var located = call("POST", plant.environment(environment) + "/container-monitors", token, "{\"deviceId\":" + container + "}");
        assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
        return container;
    }

    private static String material() {
        return "{\"code\":\"RM-%s\",\"name\":\"Active ingredient\",\"unit\":\"kg\",\"minimumStock\":1}".formatted(suffix());
    }

    private static String lot(String number) {
        return """
                {"supplier":"Supplier SAC","batchNumber":"%s","unit":"kg","amount":100,"receivedOn":"%s","expiresOn":"%s"}
                """.formatted(number, LimaDates.today(), LimaDates.today().plusYears(1));
    }

    private static String container(long containerMonitorId) {
        return "{\"containerMonitorId\":" + containerMonitorId + "}";
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Account account() throws Exception {
        var username = "container-" + UUID.randomUUID();
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
        assertThat(response.statusCode()).withFailMessage(response.body()).isIn(200, 201);
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
