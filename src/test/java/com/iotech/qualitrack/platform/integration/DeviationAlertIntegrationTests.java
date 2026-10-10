package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DeviationAlertIntegrationTests extends IntegrationTestSupport {
    @Test
    void alertIsCreatedUnresolvedAndListedForTheEquipment() throws Exception {
        var owner = readyAccount();
        long deviceId = environmentalDevice(owner);

        var alert = createAlert(owner);
        assertThat(alert.statusCode()).withFailMessage(alert.body()).isEqualTo(201);
        assertThat(json(alert, "$.status").toString()).isEqualTo("UNRESOLVED");
        assertThat(json(alert, "$.severity").toString()).isEqualTo("CRITICAL");

        var listed = call("GET", environmentPath(owner) + "/deviation-alerts?deviceId=" + deviceId, owner.token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed, "$[*].id").toString()).contains(String.valueOf(id(alert)));
    }

    @Test
    void alertIsAcknowledgedThenResolvedByTheAuthenticatedUser() throws Exception {
        var owner = readyAccount();
        environmentalDevice(owner);
        long alertId = id(createAlert(owner));

        var acknowledged = acknowledge(owner, alertId);
        assertThat(acknowledged.statusCode()).withFailMessage(acknowledged.body()).isEqualTo(200);
        assertThat(json(acknowledged, "$.status").toString()).isEqualTo("ACKNOWLEDGED");
        assertThat(((Number) json(acknowledged, "$.acknowledgedBy")).longValue()).isEqualTo(owner.userId());

        var resolved = resolve(owner, alertId, "Equipment recalibrated");
        assertThat(resolved.statusCode()).withFailMessage(resolved.body()).isEqualTo(200);
        assertThat(json(resolved, "$.status").toString()).isEqualTo("RESOLVED");
        assertThat(json(resolved, "$.resolutionNotes").toString()).isEqualTo("Equipment recalibrated");

        var stored = call("GET", "/deviation-alerts/" + alertId, owner.token(), null);
        assertThat(json(stored, "$.status").toString()).isEqualTo("RESOLVED");
    }

    @Test
    void resolvedAlertCannotChangeAgain() throws Exception {
        var owner = readyAccount();
        environmentalDevice(owner);
        long alertId = id(createAlert(owner));
        assertThat(resolve(owner, alertId, "Door closed").statusCode()).isIn(200, 201);

        assertThat(resolve(owner, alertId, "Again").statusCode()).isBetween(400, 499);
        assertThat(acknowledge(owner, alertId).statusCode()).isBetween(400, 499);
        assertThat(resolve(owner, alertId, " ").statusCode()).isEqualTo(400);
    }

    @Test
    void usersCannotActOnBehalfOfOthersOrOnForeignAlerts() throws Exception {
        var owner = readyAccount();
        var intruder = readyAccount();
        environmentalDevice(owner);
        long alertId = id(createAlert(owner));

        assertThat(acknowledge(intruder, alertId).statusCode()).isEqualTo(403);
        assertThat(resolve(intruder, alertId, "Not my alert").statusCode()).isEqualTo(403);
        assertThat(call("GET", "/deviation-alerts/" + alertId, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(json(call("GET", "/deviation-alerts/" + alertId, owner.token(), null), "$.status").toString())
                .isEqualTo("UNRESOLVED");
    }

    private HttpResponse<String> createAlert(ReadyAccount owner) throws Exception {
        return call("POST", environmentPath(owner) + "/deviation-alerts", owner.token(), """
                {"parameterName":"TEMPERATURE","recordedValue":9.5,"thresholdValue":8.0,"unit":"Celsius",
                 "detectedAt":"2026-05-12T11:00:00Z","severity":"CRITICAL"}
                """);
    }

    private HttpResponse<String> acknowledge(ReadyAccount actor, long alertId) throws Exception {
        return call("POST", "/deviation-alerts/" + alertId + "/acknowledgements", actor.token(), null);
    }

    private HttpResponse<String> resolve(ReadyAccount actor, long alertId, String notes) throws Exception {
        return call("POST", "/deviation-alerts/" + alertId + "/resolutions", actor.token(),
                "{\"resolutionNotes\":\"" + notes + "\"}");
    }
}
