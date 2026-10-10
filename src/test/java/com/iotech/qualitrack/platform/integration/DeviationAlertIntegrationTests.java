package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviationAlertIntegrationTests extends IntegrationTestSupport {
    @Test
    void alertIsCreatedUnresolvedAndListedForTheEquipment() throws Exception {
        var owner = readyAccount();
        long equipmentId = equipment(owner);

        var alert = createAlert(owner, equipmentId);
        assertThat(alert.statusCode()).withFailMessage(alert.body()).isEqualTo(201);
        assertThat(json(alert, "$.status").toString()).isEqualTo("UNRESOLVED");
        assertThat(json(alert, "$.severity").toString()).isEqualTo("CRITICAL");

        var listed = call("GET", "/equipments/" + equipmentId + "/deviation-alerts", owner.token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed, "$[*].id").toString()).contains(String.valueOf(id(alert)));
    }

    @Test
    void alertIsAcknowledgedThenResolvedByTheAuthenticatedUser() throws Exception {
        var owner = readyAccount();
        long alertId = id(createAlert(owner, equipment(owner)));

        var acknowledged = updateAlert(owner, alertId, "ACKNOWLEDGED", owner.userId(), null);
        assertThat(acknowledged.statusCode()).withFailMessage(acknowledged.body()).isEqualTo(200);
        assertThat(json(acknowledged, "$.status").toString()).isEqualTo("ACKNOWLEDGED");
        assertThat(((Number) json(acknowledged, "$.acknowledgedBy")).longValue()).isEqualTo(owner.userId());

        var resolved = updateAlert(owner, alertId, "RESOLVED", owner.userId(), "Equipment recalibrated");
        assertThat(resolved.statusCode()).withFailMessage(resolved.body()).isEqualTo(200);
        assertThat(json(resolved, "$.status").toString()).isEqualTo("RESOLVED");
        assertThat(json(resolved, "$.resolutionNotes").toString()).isEqualTo("Equipment recalibrated");

        var stored = call("GET", "/deviation-alerts/" + alertId, owner.token(), null);
        assertThat(json(stored, "$.status").toString()).isEqualTo("RESOLVED");
    }

    @Test
    void resolvedAlertCannotChangeAgain() throws Exception {
        var owner = readyAccount();
        long alertId = id(createAlert(owner, equipment(owner)));
        assertThat(updateAlert(owner, alertId, "RESOLVED", owner.userId(), "Door closed").statusCode()).isEqualTo(200);

        assertThat(updateAlert(owner, alertId, "RESOLVED", owner.userId(), "Again").statusCode()).isBetween(400, 499);
        assertThat(updateAlert(owner, alertId, "ACKNOWLEDGED", owner.userId(), null).statusCode()).isBetween(400, 499);
        assertThat(updateAlert(owner, alertId, "UNRESOLVED", owner.userId(), null).statusCode()).isEqualTo(400);
    }

    @Test
    void usersCannotActOnBehalfOfOthersOrOnForeignAlerts() throws Exception {
        var owner = readyAccount();
        var intruder = readyAccount();
        long alertId = id(createAlert(owner, equipment(owner)));

        assertThat(updateAlert(owner, alertId, "ACKNOWLEDGED", intruder.userId(), null).statusCode()).isEqualTo(403);
        assertThat(updateAlert(intruder, alertId, "ACKNOWLEDGED", intruder.userId(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/deviation-alerts/" + alertId, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(json(call("GET", "/deviation-alerts/" + alertId, owner.token(), null), "$.status").toString())
                .isEqualTo("UNRESOLVED");
    }

    private long equipment(ReadyAccount owner) throws Exception {
        var created = call("POST", "/equipments", owner.token(), """
                {"laboratoryId":%d,"name":"Cold storage A","type":"REFRIGERATOR","model":"R-200","serialNumber":"SN-%s"}
                """.formatted(owner.laboratoryId(), UUID.randomUUID()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        return id(created);
    }

    private HttpResponse<String> createAlert(ReadyAccount owner, long equipmentId) throws Exception {
        return call("POST", "/equipments/" + equipmentId + "/deviation-alerts", owner.token(), """
                {"parameterName":"TEMPERATURE","recordedValue":9.5,"thresholdValue":8.0,"unit":"Celsius",
                 "timestamp":"2026-05-12T11:00:00Z","severity":"CRITICAL"}
                """);
    }

    private HttpResponse<String> updateAlert(ReadyAccount actor, long alertId, String status, Long performedBy,
                                             String notes) throws Exception {
        var resolutionNotes = notes == null ? "null" : "\"" + notes + "\"";
        return call("PATCH", "/deviation-alerts/" + alertId, actor.token(), """
                {"status":"%s","performedBy":%d,"resolutionNotes":%s}
                """.formatted(status, performedBy, resolutionNotes));
    }
}
