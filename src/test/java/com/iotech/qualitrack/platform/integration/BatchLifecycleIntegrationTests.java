package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BatchLifecycleIntegrationTests extends IntegrationTestSupport {
    @Test
    void batchIsCreatedPendingForAProductOfTheLaboratory() throws Exception {
        var owner = readyAccount();
        long productId = product(owner);

        var batch = createBatch(owner, productId);
        assertThat(batch.statusCode()).withFailMessage(batch.body()).isEqualTo(201);
        assertThat(json(batch, "$.status").toString()).isEqualTo("PENDING");
        assertThat(json(batch, "$.productName").toString()).isEqualTo("Ibuprofen 400mg");

        var byLaboratory = call("GET", "/laboratories/" + owner.laboratoryId() + "/batches", owner.token(), null);
        assertThat(byLaboratory.statusCode()).isEqualTo(200);
        assertThat(json(byLaboratory, "$[*].id").toString()).contains(String.valueOf(id(batch)));
    }

    @Test
    void releasedBatchIsPersistedAndCannotBeRejectedLater() throws Exception {
        var owner = readyAccount();
        long batchId = id(createBatch(owner, product(owner)));

        var released = call("PATCH", "/batches/" + batchId, owner.token(), """
                {"status":"RELEASED","releaseDate":"2026-05-20","notes":"All quality controls passed"}
                """);
        assertThat(released.statusCode()).withFailMessage(released.body()).isEqualTo(200);
        assertThat(json(released, "$.status").toString()).isEqualTo("RELEASED");

        var rejected = call("PATCH", "/batches/" + batchId, owner.token(), """
                {"status":"REJECTED","rejectionDate":"2026-05-21","reason":"Late failure"}
                """);
        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(json(call("GET", "/batches/" + batchId, owner.token(), null), "$.status").toString()).isEqualTo("RELEASED");
    }

    @Test
    void invalidBatchRequestsAreRejected() throws Exception {
        var owner = readyAccount();
        long productId = product(owner);
        long batchId = id(createBatch(owner, productId));

        var negativeQuantity = call("POST", "/batches", owner.token(), """
                {"labId":%d,"productId":%d,"batchNumber":"LOT-%s","quantity":-5,"unit":"units","startDate":"2026-05-12"}
                """.formatted(owner.laboratoryId(), productId, UUID.randomUUID()));
        var unsupportedStatus = call("PATCH", "/batches/" + batchId, owner.token(), "{\"status\":\"IN_PROGRESS\"}");
        var missingBatch = call("GET", "/batches/999999", owner.token(), null);

        assertThat(negativeQuantity.statusCode()).isEqualTo(400);
        assertThat(unsupportedStatus.statusCode()).isEqualTo(400);
        assertThat(missingBatch.statusCode()).isIn(403, 404);
    }

    @Test
    void anotherLaboratoryCannotReadOrReleaseTheBatch() throws Exception {
        var owner = readyAccount();
        var intruder = readyAccount();
        long batchId = id(createBatch(owner, product(owner)));

        assertThat(call("GET", "/batches/" + batchId, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("PATCH", "/batches/" + batchId, intruder.token(), """
                {"status":"RELEASED","releaseDate":"2026-05-20","notes":"Not my batch"}
                """).statusCode()).isEqualTo(403);
        assertThat(json(call("GET", "/batches/" + batchId, owner.token(), null), "$.status").toString()).isEqualTo("PENDING");
    }

    private long product(ReadyAccount owner) throws Exception {
        var created = call("POST", "/laboratories/" + owner.laboratoryId() + "/products", owner.token(), """
                {"name":"Ibuprofen 400mg","code":"P-%s","description":"Tablets","specifications":"400mg"}
                """.formatted(UUID.randomUUID()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        var products = call("GET", "/laboratories/" + owner.laboratoryId() + "/products", owner.token(), null);
        return ((Number) json(products, "$[0].id")).longValue();
    }

    private HttpResponse<String> createBatch(ReadyAccount owner, long productId) throws Exception {
        return call("POST", "/batches", owner.token(), """
                {"labId":%d,"productId":%d,"batchNumber":"LOT-%s","quantity":5000,"unit":"units",
                 "startDate":"2026-05-12","notes":"Standard run"}
                """.formatted(owner.laboratoryId(), productId, UUID.randomUUID()));
    }
}
