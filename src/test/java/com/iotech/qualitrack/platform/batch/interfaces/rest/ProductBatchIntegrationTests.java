package com.iotech.qualitrack.platform.batch.interfaces.rest;

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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the Product Batch Management API (TS61-TS72) through HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductBatchIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired IamContextFacade iam;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository equipmentRepository;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20700000000L);

    private record Account(Long id, String token) { }

    private record Plant(Account manager, long lab, long production) {
        String products() { return "/laboratories/" + lab + "/environments/" + production + "/products"; }
    }

    @Test
    void qualityManagerRegistersListsAndReadsProductsOfAnEnvironment() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        assertThat(call("GET", plant.products(), token, null).body()).isEqualTo("[]");

        var created = call("POST", plant.products(), token, product("PRD-ASP-500", "Aspirin 500mg"));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long productId = id(created);
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith(plant.products() + "/" + productId));
        assertThat(created.body()).contains("\"environmentId\":" + plant.production()).contains("\"active\":true");

        assertThat(JsonPath.<List<Integer>>read(call("GET", plant.products(), token, null).body(), "$[*].id"))
                .containsExactly((int) productId);
        assertThat(call("GET", plant.products() + "/" + productId, token, null).statusCode()).isEqualTo(200);

        long storage = environment(plant, "WH-PT");
        var otherEnvironment = "/laboratories/" + plant.lab() + "/environments/" + storage + "/products";
        assertThat(call("GET", otherEnvironment, token, null).body()).isEqualTo("[]");
        assertThat(call("GET", otherEnvironment + "/" + productId, token, null).statusCode()).isEqualTo(404);
    }

    @Test
    void productCodesAndNamesAreUniqueInTheLaboratoryOnly() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        assertThat(call("POST", plant.products(), token, product("PRD-1", "Ibuprofen 400mg")).statusCode()).isEqualTo(201);

        long storage = environment(plant, "WH-PT");
        var otherEnvironment = "/laboratories/" + plant.lab() + "/environments/" + storage + "/products";
        assertThat(call("POST", otherEnvironment, token, product("PRD-1", "Other name")).statusCode()).isEqualTo(409);
        assertThat(call("POST", plant.products(), token, product("PRD-2", "Ibuprofen 400mg")).statusCode()).isEqualTo(409);
        assertThat(call("POST", plant.products(), token, product("PRD-3", " ")).statusCode()).isEqualTo(400);

        var otherPlant = plant();
        assertThat(call("POST", otherPlant.products(), otherPlant.manager().token(), product("PRD-1", "Ibuprofen 400mg"))
                .statusCode()).isEqualTo(201);
    }

    @Test
    void onlyQualityRolesRegisterProductsAndOtherLaboratoriesCannotReadThem() throws Exception {
        var plant = plant();
        var operator = operatorOf(plant);
        assertThat(call("POST", plant.products(), operator.token(), product("PRD-OP", "Operator product")).statusCode()).isEqualTo(403);
        assertThat(call("GET", plant.products(), operator.token(), null).statusCode()).isEqualTo(200);

        var outsider = plant();
        assertThat(call("GET", plant.products(), outsider.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void laboratoryStaffRegistersBatchesOfAProduct() throws Exception {
        var plant = plant();
        var batches = plant.products() + "/" + id(call("POST", plant.products(), plant.manager().token(), product("PRD-B", "Batch product"))) + "/batches";
        var operator = operatorOf(plant);

        var created = call("POST", batches, operator.token(), batch("PB-2026-001", "2026-10-01"));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long batchId = id(created);
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location -> assertThat(location).endsWith(batches + "/" + batchId));
        assertThat(created.body()).contains("\"status\":\"PENDING\"").contains("\"environmentId\":" + plant.production())
                .contains("\"productName\":\"Batch product\"");
        assertThat(call("POST", batches, operator.token(), batch("PB-2026-002", "2026-10-02")).statusCode()).isEqualTo(201);

        assertThat(JsonPath.<List<String>>read(call("GET", batches, operator.token(), null).body(), "$[*].batchNumber"))
                .containsExactly("PB-2026-002", "PB-2026-001");
        assertThat(call("GET", batches + "/" + batchId, operator.token(), null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/laboratories/" + plant.lab() + "/batches", operator.token(), null).body()).contains("PB-2026-001");

        var otherProduct = plant.products() + "/" + id(call("POST", plant.products(), plant.manager().token(), product("PRD-C", "Other product"))) + "/batches";
        assertThat(call("POST", otherProduct, operator.token(), batch("PB-2026-001", "2026-10-03")).statusCode()).isEqualTo(409);
        assertThat(call("GET", otherProduct + "/" + batchId, operator.token(), null).statusCode()).isEqualTo(404);
        assertThat(call("POST", batches, operator.token(), batch("PB-BAD-DATE", "10/02/2026")).statusCode()).isEqualTo(400);

        long storage = environment(plant, "WH-PT");
        var wrongEnvironment = "/laboratories/" + plant.lab() + "/environments/" + storage + "/products/"
                + batches.split("/products/")[1];
        assertThat(call("GET", wrongEnvironment, operator.token(), null).statusCode()).isEqualTo(404);
        assertThat(call("POST", wrongEnvironment, operator.token(), batch("PB-X", "2026-10-03")).statusCode()).isEqualTo(404);

        var otherPlant = plant();
        var otherBatches = otherPlant.products() + "/" + id(call("POST", otherPlant.products(), otherPlant.manager().token(),
                product("PRD-B", "Batch product"))) + "/batches";
        assertThat(call("POST", otherBatches, otherPlant.manager().token(), batch("PB-2026-001", "2026-10-01")).statusCode()).isEqualTo(201);
        assertThat(call("GET", batches, otherPlant.manager().token(), null).statusCode()).isEqualTo(403);
    }

    @Test
    void qualityRolesReleaseOrRejectBatchesOnce() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var batches = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-R", "Release product"))) + "/batches";
        long released = id(call("POST", batches, token, batch("PB-REL", "2026-10-01")));
        long rejected = id(call("POST", batches, token, batch("PB-REJ", "2026-10-01")));
        var operator = operatorOf(plant);
        var release = "{\"releaseDate\":\"2026-10-05\",\"notes\":\"All controls passed\"}";
        var rejection = "{\"rejectionDate\":\"2026-10-05\",\"reason\":\"Out of specification\"}";

        assertThat(call("POST", batches + "/" + released + "/releases", operator.token(), release).statusCode()).isEqualTo(403);
        var signed = call("POST", batches + "/" + released + "/releases", token, release);
        assertThat(signed.statusCode()).withFailMessage(signed.body()).isEqualTo(201);
        assertThat(signed.body()).contains("\"status\":\"RELEASED\"").contains("\"releaseDate\":\"2026-10-05\"")
                .contains("\"releasedByUserId\":" + plant.manager().id());
        assertThat(JsonPath.<String>read(signed.body(), "$.signatureHash")).matches("[0-9a-f]{64}");
        assertThat(call("POST", batches + "/" + released + "/releases", token, release).statusCode()).isEqualTo(409);
        assertThat(call("POST", batches + "/" + released + "/rejections", token, rejection).statusCode()).isEqualTo(409);

        assertThat(call("POST", batches + "/" + rejected + "/rejections", operator.token(), rejection).statusCode()).isEqualTo(403);
        var record = call("POST", batches + "/" + rejected + "/rejections", token, rejection);
        assertThat(record.statusCode()).withFailMessage(record.body()).isEqualTo(201);
        assertThat(record.body()).contains("\"status\":\"REJECTED\"").contains("\"reason\":\"Out of specification\"");
        assertThat(call("POST", batches + "/" + rejected + "/releases", token, release).statusCode()).isEqualTo(409);
        assertThat(call("GET", batches + "/" + rejected, token, null).body()).contains("\"status\":\"REJECTED\"");
        assertThat(call("POST", batches + "/" + rejected + "/rejections", token, "{\"rejectionDate\":\"2026-10-05\",\"reason\":\" \"}")
                .statusCode()).isEqualTo(400);
        assertThat(call("POST", batches + "/999999/releases", token, release).statusCode()).isIn(403, 404);
    }

    @Test
    void batchesConsumeReleasedLotsOnceAndOnlyWhileOpen() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var lot = releasedLot(plant, "100");
        var batches = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-U", "Usage product"))) + "/batches";
        long batchId = id(call("POST", batches, token, batch("PB-USE", "2026-10-01")));
        var usages = batches + "/" + batchId + "/raw-material-usages";
        var operator = operatorOf(plant);
        var body = usage(lot.id(), "30", "kg", "op-" + UUID.randomUUID());
        assertThat(call("GET", batches + "/" + batchId, token, null).body()).contains("\"status\":\"PENDING\"");

        var created = call("POST", usages, operator.token(), body);
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"inventoryReceiptId\":" + lot.id()).contains("\"rawMaterialId\":" + lot.material());
        assertThat(((Number) JsonPath.read(created.body(), "$.stockAfter")).doubleValue()).isEqualTo(70);
        assertThat(call("GET", batches + "/" + batchId, token, null).body()).contains("\"status\":\"IN_PROGRESS\"");
        var retried = call("POST", usages, operator.token(), body);
        assertThat(retried.statusCode()).isEqualTo(201);
        assertThat(id(retried)).isEqualTo(id(created));
        assertThat(usableStock(plant, lot)).isEqualTo(70);
        assertThat(call("POST", usages, operator.token(), usage(lot.id(), "5", "kg", "op-" + UUID.randomUUID())).statusCode()).isEqualTo(201);
        var auditLogs = call("GET", "/batches/" + batchId + "/audit-logs", token, null).body();
        assertThat(auditLogs.split("BatchStartedIntegrationEvent", -1)).hasSize(2);
        assertThat(usableStock(plant, lot)).isEqualTo(65);

        assertThat(call("POST", usages, operator.token(), usage(lot.id(), "500", "kg", "op-" + UUID.randomUUID())).statusCode()).isEqualTo(409);
        assertThat(call("POST", usages, operator.token(), usage(lot.id(), "1", "L", "op-" + UUID.randomUUID())).statusCode()).isEqualTo(400);
        var otherPlant = plant();
        var foreignLot = releasedLot(otherPlant, "10");
        assertThat(call("POST", usages, operator.token(), usage(foreignLot.id(), "1", "kg", "op-" + UUID.randomUUID())).statusCode()).isEqualTo(404);

        assertThat(call("POST", batches + "/" + batchId + "/releases", token, "{\"releaseDate\":\"2026-10-05\",\"notes\":\"Approved\"}")
                .statusCode()).isEqualTo(201);
        assertThat(call("POST", usages, operator.token(), usage(lot.id(), "1", "kg", "op-" + UUID.randomUUID())).statusCode()).isEqualTo(409);
        assertThat(usableStock(plant, lot)).isEqualTo(65);
    }

    @Test
    void batchesRecordOperationalEquipmentAndRegisteredStaffOnce() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var batches = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-P", "Participation product"))) + "/batches";
        long batchId = id(call("POST", batches, token, batch("PB-PART", "2026-10-01")));
        var operatorMember = staffOperator(plant, "Plant operator");
        var operator = new Account(operatorMember.userId(), operatorMember.token());
        long press = equipment(plant, "Tablet press");
        long mixer = equipment(plant, "Mixer");
        var underMaintenance = equipmentRepository.findById(mixer).orElseThrow();
        underMaintenance.updateStatus(com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus.MAINTENANCE);
        equipmentRepository.save(underMaintenance);
        long staffId = staff(plant, "Ana Torres", "Production operator");

        var equipmentUsages = batches + "/" + batchId + "/equipment-usages";
        var used = call("POST", equipmentUsages, operator.token(), "{\"equipmentId\":" + press + "}");
        assertThat(used.statusCode()).withFailMessage(used.body()).isEqualTo(201);
        assertThat(used.body()).contains("\"equipmentName\":\"Tablet press\"").contains("\"registeredByUserId\":" + operator.id());
        assertThat(call("POST", equipmentUsages, operator.token(), "{\"equipmentId\":" + press + "}").statusCode()).isEqualTo(409);
        var maintenance = call("POST", equipmentUsages, operator.token(), "{\"equipmentId\":" + mixer + "}");
        assertThat(maintenance.statusCode()).isEqualTo(409);
        assertThat(maintenance.body()).contains("MAINTENANCE");
        assertThat(call("POST", equipmentUsages, operator.token(), "{\"equipmentId\":999999}").statusCode()).isEqualTo(404);

        var participations = batches + "/" + batchId + "/staff-participations";
        assertThat(call("POST", participations, operator.token(), "{\"staffId\":" + staffId + "}").statusCode()).isEqualTo(403);
        var participated = call("POST", participations, token, "{\"staffId\":" + staffId + "}");
        assertThat(participated.statusCode()).withFailMessage(participated.body()).isEqualTo(201);
        assertThat(participated.body()).contains("\"staffName\":\"Ana Torres\"").contains("\"staffRole\":\"Production operator\"");
        var himself = call("POST", participations, operator.token(), "{\"staffId\":" + operatorMember.staffId() + "}");
        assertThat(himself.statusCode()).withFailMessage(himself.body()).isEqualTo(201);
        assertThat(call("POST", participations, token, "{\"staffId\":" + staffId + "}").statusCode()).isEqualTo(409);
        assertThat(call("POST", participations, token, "{\"staffId\":999999}").statusCode()).isEqualTo(404);
        var auditor = TestStaff.register(this::call, plant.lab(), token, "Batch auditor", "AUDITOR");
        assertThat(call("POST", participations, token, "{\"staffId\":" + auditor.staffId() + "}").statusCode()).isEqualTo(400);
        var outsider = plant();
        long foreignStaff = staff(outsider, "Luis Ramos", "Analyst");
        assertThat(call("POST", participations, token, "{\"staffId\":" + foreignStaff + "}").statusCode()).isEqualTo(404);

        call("POST", batches + "/" + batchId + "/rejections", token, "{\"rejectionDate\":\"2026-10-05\",\"reason\":\"Contamination\"}");
        assertThat(call("POST", equipmentUsages, operator.token(), "{\"equipmentId\":" + equipment(plant, "Coater") + "}").statusCode()).isEqualTo(409);
        assertThat(call("POST", participations, token, "{\"staffId\":" + staff(plant, "Rosa Diaz", "Operator") + "}").statusCode()).isEqualTo(409);
    }

    @Test
    void traceabilityShowsEverythingThatTookPartInTheBatch() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var lot = releasedLot(plant, "100");
        long productId = id(call("POST", plant.products(), token, product("PRD-T", "Traced product")));
        var batches = plant.products() + "/" + productId + "/batches";
        long batchId = id(call("POST", batches, token, batch("PB-TRACE", "2026-10-01")));
        var batch = batches + "/" + batchId;
        call("POST", batch + "/raw-material-usages", token, usage(lot.id(), "25", "kg", "op-" + UUID.randomUUID()));
        call("POST", batch + "/equipment-usages", token, "{\"equipmentId\":" + equipment(plant, "Granulator") + "}");
        call("POST", batch + "/staff-participations", token, "{\"staffId\":" + staff(plant, "Ana Torres", "Supervisor") + "}");

        var open = call("GET", batch + "/traceability", operatorOf(plant).token(), null);
        assertThat(open.statusCode()).withFailMessage(open.body()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(open.body(), "$.batch.id")).isEqualTo((int) batchId);
        assertThat(JsonPath.<String>read(open.body(), "$.product.code")).isEqualTo("PRD-T");
        assertThat(JsonPath.<Integer>read(open.body(), "$.rawMaterials[0].inventoryReceiptId")).isEqualTo((int) lot.id());
        assertThat(JsonPath.<Integer>read(open.body(), "$.rawMaterials[0].rawMaterialEnvironmentId")).isEqualTo((int) lot.environment());
        assertThat(JsonPath.<String>read(open.body(), "$.equipment[0].equipmentName")).isEqualTo("Granulator");
        assertThat(JsonPath.<String>read(open.body(), "$.staff[0].staffName")).isEqualTo("Ana Torres");
        assertThat(open.body()).contains("\"release\":null").contains("\"rejection\":null");

        call("POST", batch + "/releases", token, "{\"releaseDate\":\"2026-10-05\",\"notes\":\"Approved\"}");
        var released = call("GET", batch + "/traceability", token, null);
        assertThat(JsonPath.<String>read(released.body(), "$.release.signatureHash")).matches("[0-9a-f]{64}");
        assertThat(JsonPath.<Integer>read(released.body(), "$.release.signedByUserId")).isEqualTo(plant.manager().id().intValue());

        long otherProduct = id(call("POST", plant.products(), token, product("PRD-T2", "Other traced product")));
        assertThat(call("GET", plant.products() + "/" + otherProduct + "/batches/" + batchId + "/traceability", token, null)
                .statusCode()).isEqualTo(404);
    }

    @Test
    void laboratoryListShowsTheBatchesOfEveryProductOfTheLaboratoryOnly() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var list = "/laboratories/" + plant.lab() + "/batches";
        assertThat(call("GET", list, token, null).body()).isEqualTo("[]");

        var tablets = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-LIST-T", "Listed tablets"))) + "/batches";
        long liquids = environment(plant, "PROD-LIQ");
        var liquidProducts = "/laboratories/" + plant.lab() + "/environments/" + liquids + "/products";
        var syrups = liquidProducts + "/" + id(call("POST", liquidProducts, token, product("PRD-LIST-S", "Listed syrup"))) + "/batches";
        assertThat(call("POST", tablets, token, batch("PB-LIST-T", "2026-10-01")).statusCode()).isEqualTo(201);
        assertThat(call("POST", syrups, token, batch("PB-LIST-S", "2026-10-02")).statusCode()).isEqualTo(201);

        var listed = call("GET", list, operatorOf(plant).token(), null);
        assertThat(listed.statusCode()).withFailMessage(listed.body()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(listed.body(), "$[*].batchNumber")).containsExactlyInAnyOrder("PB-LIST-T", "PB-LIST-S");
        assertThat(JsonPath.<List<Integer>>read(listed.body(), "$[*].environmentId"))
                .containsExactlyInAnyOrder((int) plant.production(), (int) liquids);

        var auditor = TestStaff.register(this::call, plant.lab(), token, "List auditor", "AUDITOR");
        assertThat(call("GET", list, auditor.token(), null).statusCode()).isEqualTo(200);
        assertThat(call("POST", tablets, auditor.token(), batch("PB-LIST-A", "2026-10-03")).statusCode()).isEqualTo(403);

        var outsider = plant();
        assertThat(call("GET", list, outsider.manager().token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/laboratories/" + outsider.lab() + "/batches", outsider.manager().token(), null).body()).isEqualTo("[]");
        assertThat(call("GET", "/laboratories/not-a-number/batches", token, null).statusCode()).isEqualTo(400);
    }

    @Test
    void invalidBatchRequestsAreRejectedWithoutSavingAnything() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var batches = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-V", "Validated product"))) + "/batches";
        var invalid = List.of(
                "{\"batchNumber\":\"PB-ZERO\",\"quantity\":0,\"unit\":\"units\",\"startDate\":\"2026-10-01\"}",
                "{\"batchNumber\":\"PB-NEGATIVE\",\"quantity\":-5,\"unit\":\"units\",\"startDate\":\"2026-10-01\"}",
                "{\"batchNumber\":\"" + "B".repeat(51) + "\",\"quantity\":10,\"unit\":\"units\",\"startDate\":\"2026-10-01\"}",
                "{\"batchNumber\":\"PB-BOXES\",\"quantity\":10,\"unit\":\"boxes\",\"startDate\":\"2026-10-01\"}",
                "{\"batchNumber\":\"PB-FEB-30\",\"quantity\":10,\"unit\":\"units\",\"startDate\":\"2026-02-30\"}",
                "{\"batchNumber\":\"PB-NO-DATE\",\"quantity\":10,\"unit\":\"units\"}",
                "{\"batchNumber\":\"PB-NOTES\",\"quantity\":10,\"unit\":\"units\",\"startDate\":\"2026-10-01\",\"notes\":\""
                        + "N".repeat(501) + "\"}");
        for (var body : invalid) {
            var response = call("POST", batches, token, body);
            assertThat(response.statusCode()).withFailMessage("%s -> %s", body, response.body()).isEqualTo(400);
        }

        long batchId = id(call("POST", batches, token, batch("PB-VALID", "2026-10-01")));
        var batch = batches + "/" + batchId;
        assertThat(call("POST", batch + "/releases", token, "{\"releaseDate\":\"2026-10-05\",\"notes\":\" \"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", batch + "/releases", token, "{\"releaseDate\":\"05/10/2026\",\"notes\":\"Approved\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", batch + "/rejections", token, "{\"rejectionDate\":\"2026-10-05\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", batch + "/rejections", token, "{\"rejectionDate\":\"2026-13-01\",\"reason\":\"Contamination\"}")
                .statusCode()).isEqualTo(400);

        assertThat(JsonPath.<List<String>>read(call("GET", batches, token, null).body(), "$[*].batchNumber")).containsExactly("PB-VALID");
        assertThat(call("GET", batch, token, null).body()).contains("\"status\":\"PENDING\"");
    }

    @Test
    void traceabilityKeepsTheRejectionAndStaysInTheLaboratory() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var batches = plant.products() + "/" + id(call("POST", plant.products(), token, product("PRD-TR", "Rejected product"))) + "/batches";
        long batchId = id(call("POST", batches, token, batch("PB-TR-REJ", "2026-10-01")));
        var traceability = batches + "/" + batchId + "/traceability";
        var rejected = call("POST", batches + "/" + batchId + "/rejections", token,
                "{\"rejectionDate\":\"2026-10-06\",\"reason\":\"  Out of specification  \"}");
        assertThat(rejected.statusCode()).withFailMessage(rejected.body()).isEqualTo(201);

        var traced = call("GET", traceability, token, null);
        assertThat(traced.statusCode()).withFailMessage(traced.body()).isEqualTo(200);
        assertThat(JsonPath.<String>read(traced.body(), "$.batch.status")).isEqualTo("REJECTED");
        assertThat(JsonPath.<String>read(traced.body(), "$.rejection.rejectionDate")).isEqualTo("2026-10-06");
        assertThat(JsonPath.<String>read(traced.body(), "$.rejection.reason")).isEqualTo("Out of specification");
        assertThat(traced.body()).contains("\"release\":null");

        var auditor = TestStaff.register(this::call, plant.lab(), token, "Traceability auditor", "AUDITOR");
        assertThat(call("GET", traceability, auditor.token(), null).statusCode()).isEqualTo(200);
        var outsider = plant();
        assertThat(call("GET", traceability, outsider.manager().token(), null).statusCode()).isEqualTo(403);
    }

    private long equipment(Plant plant, String name) throws Exception {
        var created = call("POST", "/laboratories/" + plant.lab() + "/equipments", plant.manager().token(), """
                {"name":"%s","type":"PRODUCTION","model":"M-1","serialNumber":"SN-%s"}
                """.formatted(name, UUID.randomUUID().toString().substring(0, 12)));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private long staff(Plant plant, String fullName, String role) throws Exception {
        var created = call("POST", "/laboratories/" + plant.lab() + "/staff", plant.manager().token(), """
                {"fullName":"%s","role":"%s","email":"%s@qualitrack.test","accessRole":"OPERATOR"}
                """.formatted(fullName, role, UUID.randomUUID()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return ((Number) JsonPath.read(created.body(), "$.staffMember.id")).longValue();
    }

    private record Lot(long environment, long material, long id) { }

    private Lot releasedLot(Plant plant, String amount) throws Exception {
        var token = plant.manager().token();
        long storage = environment(plant, "WH-RM-" + UUID.randomUUID().toString().substring(0, 6));
        var materials = "/laboratories/" + plant.lab() + "/environments/" + storage + "/raw-materials";
        long material = id(call("POST", materials, token,
                "{\"code\":\"RM-" + UUID.randomUUID().toString().substring(0, 8) + "\",\"name\":\"Active ingredient\",\"unit\":\"kg\",\"minimumStock\":1}"));
        var received = call("POST", materials + "/" + material + "/batches", token, """
                {"supplier":"Supplier","batchNumber":"SUP-1","unit":"kg","amount":%s,"receivedOn":"%s","expiresOn":"%s"}
                """.formatted(amount, LimaDates.today().minusDays(1), LimaDates.today().plusYears(1)));
        assertThat(received.statusCode()).withFailMessage(received.body()).isEqualTo(201);
        long lot = id(received);
        assertThat(call("POST", materials + "/" + material + "/batches/" + lot + "/reviews", token,
                "{\"status\":\"RELEASED\",\"reason\":\"Certificate reviewed\"}").statusCode()).isEqualTo(201);
        return new Lot(storage, material, lot);
    }

    private double usableStock(Plant plant, Lot lot) throws Exception {
        var material = call("GET", "/laboratories/" + plant.lab() + "/environments/" + lot.environment() + "/raw-materials/" + lot.material(),
                plant.manager().token(), null);
        return ((Number) JsonPath.read(material.body(), "$.usableStock")).doubleValue();
    }

    private static String usage(long lot, String amount, String unit, String operationId) {
        return "{\"rawMaterialBatchId\":%d,\"amountUsed\":%s,\"unit\":\"%s\",\"operationId\":\"%s\"}".formatted(lot, amount, unit, operationId);
    }

    private static String batch(String number, String startDate) {
        return """
                {"batchNumber":"%s","quantity":5000,"unit":"units","startDate":"%s","notes":"Standard run"}
                """.formatted(number, startDate);
    }

    private static String product(String code, String name) {
        return """
                {"code":"%s","name":"%s","description":"Test product","specifications":"Tablet, blister pack"}
                """.formatted(code, name);
    }

    private Plant plant() throws Exception {
        var manager = account("ROLE_QA_MANAGER");
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Product laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        var production = call("POST", "/laboratories/" + lab + "/environments", manager.token(),
                "{\"code\":\"PROD-01\",\"name\":\"Production area\"}");
        assertThat(production.statusCode()).withFailMessage(production.body()).isEqualTo(201);
        return new Plant(manager, lab, id(production));
    }

    private long environment(Plant plant, String code) throws Exception {
        var created = call("POST", "/laboratories/" + plant.lab() + "/environments", plant.manager().token(),
                "{\"code\":\"" + code + "\",\"name\":\"Environment " + code + "\"}");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private Account operatorOf(Plant plant) throws Exception {
        var operator = staffOperator(plant, "Plant operator");
        return new Account(operator.userId(), operator.token());
    }

    private TestStaff.Member staffOperator(Plant plant, String fullName) throws Exception {
        return TestStaff.register(this::call, plant.lab(), plant.manager().token(), fullName, "OPERATOR");
    }

    private Account account(String role) throws Exception {
        var username = "product-" + UUID.randomUUID();
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
