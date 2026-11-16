package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.testing.LimaDates;
import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the Equipment and IoT device API (TS31-TS41) through HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EquipmentIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired IamContextFacade iam;
    @Autowired AuditLogRepository audit;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20800000000L);

    private record Account(Long id, String token) { }

    private record Plant(Account manager, long lab, long storage) {
        String equipment() { return "/laboratories/" + lab + "/equipments"; }
        String environment(long environmentId) { return "/laboratories/" + lab + "/environments/" + environmentId; }
    }

    @Test
    void qualityManagerRegistersAndListsEquipmentOfTheLaboratory() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        assertThat(call("GET", plant.equipment(), token, null).body()).isEqualTo("[]");

        var created = call("POST", plant.equipment(), token, equipment("Tablet press", "SN-PRESS-" + suffix()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long pressId = id(created);
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith(plant.equipment() + "/" + pressId));
        assertThat(created.body()).contains("\"status\":\"OPERATIONAL\"").contains("\"environmentId\":null")
                .contains("\"deviceType\":null");

        var listed = call("GET", plant.equipment(), token, null);
        assertThat(JsonPath.<List<Integer>>read(listed.body(), "$[*].id")).containsExactly((int) pressId);
        assertThat(call("GET", plant.equipment() + "/" + pressId, token, null).statusCode()).isEqualTo(200);

        var serial = JsonPath.<String>read(created.body(), "$.serialNumber");
        assertThat(call("POST", plant.equipment(), token, equipment("Second press", serial)).statusCode()).isEqualTo(409);
        assertThat(call("POST", plant.equipment(), token, equipment(" ", "SN-" + suffix())).statusCode()).isEqualTo(400);

        var operator = operatorOf(plant);
        assertThat(call("GET", plant.equipment(), operator.token(), null).statusCode()).isEqualTo(200);
        assertThat(call("POST", plant.equipment(), operator.token(), equipment("Mixer", "SN-" + suffix())).statusCode()).isEqualTo(403);

        var foreign = plant();
        assertThat(call("GET", foreign.equipment(), token, null).statusCode()).isEqualTo(403);
        assertThat(call("GET", foreign.equipment() + "/" + pressId, foreign.manager().token(), null).statusCode()).isIn(403, 404);
    }

    @Test
    void equipmentIsLocatedInAnEnvironmentOfTheSameLaboratory() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long press = id(call("POST", plant.equipment(), token, equipment("Tablet press", "SN-" + suffix())));

        var located = call("POST", plant.environment(plant.storage()) + "/equipments", token, "{\"equipmentId\":" + press + "}");
        assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
        assertThat(located.body()).contains("\"environmentId\":" + plant.storage());
        assertThat(located.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith(plant.equipment() + "/" + press));

        long production = environment(plant, "PROD-" + suffix());
        var moved = call("POST", plant.environment(production) + "/equipments", token, "{\"equipmentId\":" + press + "}");
        assertThat(moved.statusCode()).isEqualTo(201);
        assertThat(moved.body()).contains("\"environmentId\":" + production);

        var foreign = plant();
        long foreignPress = id(call("POST", foreign.equipment(), foreign.manager().token(), equipment("Foreign press", "SN-" + suffix())));
        assertThat(call("POST", plant.environment(production) + "/equipments", token, "{\"equipmentId\":" + foreignPress + "}")
                .statusCode()).isEqualTo(404);
        assertThat(call("POST", plant.environment(production) + "/equipments", token, "{\"equipmentId\":999999999}")
                .statusCode()).isEqualTo(404);
        assertThat(call("POST", plant.environment(foreign.storage()) + "/equipments", token, "{\"equipmentId\":" + press + "}")
                .statusCode()).isEqualTo(403);
        assertThat(call("POST", plant.environment(production) + "/equipments", operatorOf(plant).token(), "{\"equipmentId\":" + press + "}")
                .statusCode()).isEqualTo(403);
    }

    @Test
    void statusChangesAreTraceableAndRejectedWhenNotAllowed() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long press = located(plant, plant.storage(), "Tablet press");
        var statusChanges = plant.environment(plant.storage()) + "/equipments/" + press + "/status-changes";
        var operator = operatorOf(plant);

        var changed = call("POST", statusChanges, operator.token(), "{\"status\":\"maintenance\",\"reason\":\"Scheduled calibration\"}");
        assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(201);
        assertThat(changed.body()).contains("\"previousStatus\":\"OPERATIONAL\"").contains("\"newStatus\":\"MAINTENANCE\"")
                .contains("\"changedByUserId\":" + operator.id()).contains("\"reason\":\"Scheduled calibration\"");
        assertThat(call("GET", plant.equipment() + "/" + press, token, null).body()).contains("\"status\":\"MAINTENANCE\"");
        assertThat(audit.findAllByEquipmentId(press))
                .anySatisfy(entry -> assertThat(entry.getDetails()).contains("newStatus=MAINTENANCE"));

        assertThat(call("POST", statusChanges, token, "{\"status\":\"MAINTENANCE\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", statusChanges, token, "{\"status\":\"BROKEN\"}").statusCode()).isEqualTo(400);
        assertThat(call("GET", plant.equipment() + "/" + press, token, null).body()).contains("\"status\":\"MAINTENANCE\"");

        long otherEnvironment = environment(plant, "QC-" + suffix());
        assertThat(call("POST", plant.environment(otherEnvironment) + "/equipments/" + press + "/status-changes", token,
                "{\"status\":\"OPERATIONAL\"}").statusCode()).isEqualTo(404);
    }

    @Test
    void maintenanceIsRegisteredAndListedForEquipmentOfTheEnvironment() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        long press = located(plant, plant.storage(), "Tablet press");
        var maintenance = plant.environment(plant.storage()) + "/equipments/" + press + "/maintenance-records";

        assertThat(call("GET", maintenance, token, null).body()).isEqualTo("[]");
        var operator = staffOperator(plant, "Luis Paredes");
        var colleague = staffOperator(plant, "Rosa Diaz");
        var older = call("POST", maintenance, operator.token(), maintenanceBody(LimaDates.today().minusDays(10), "INSPECTION", operator.staffId()));
        assertThat(older.statusCode()).withFailMessage(older.body()).isEqualTo(201);
        assertThat(older.body()).contains("\"environmentId\":" + plant.storage()).contains("\"type\":\"INSPECTION\"")
                .contains("\"technicianName\":\"Luis Paredes\"").contains("\"technicianStaffId\":" + operator.staffId());
        assertThat(call("POST", maintenance, operator.token(), maintenanceBody(LimaDates.today(), "INSPECTION", colleague.staffId()))
                .statusCode()).isEqualTo(403);
        assertThat(call("POST", maintenance, token, maintenanceBody(LimaDates.today(), "calibration", colleague.staffId())).statusCode())
                .isEqualTo(201);
        assertThat(call("POST", maintenance, token, maintenanceBody(LimaDates.today(), "OTHER", 999999L)).statusCode()).isEqualTo(404);

        var history = call("GET", maintenance, operator.token(), null);
        assertThat(JsonPath.<List<String>>read(history.body(), "$[*].type")).containsExactly("CALIBRATION", "INSPECTION");

        assertThat(call("POST", maintenance, token, maintenanceBody(LimaDates.today().plusDays(1), "PREVENTIVE", colleague.staffId())).statusCode()).isEqualTo(400);
        assertThat(call("POST", maintenance, token, maintenanceBody(LimaDates.today(), "PAINTING", colleague.staffId())).statusCode()).isEqualTo(400);

        long unlocated = id(call("POST", plant.equipment(), token, equipment("Unlocated mixer", "SN-" + suffix())));
        var unlocatedMaintenance = plant.environment(plant.storage()) + "/equipments/" + unlocated + "/maintenance-records";
        assertThat(call("GET", unlocatedMaintenance, token, null).statusCode()).isEqualTo(404);
        assertThat(call("POST", unlocatedMaintenance, token, maintenanceBody(LimaDates.today(), "PREVENTIVE", colleague.staffId())).statusCode()).isEqualTo(404);
    }

    @Test
    void iotDevicesHaveUniqueIdentityAndAnEnvironmentHasOneEnvironmentalDevice() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var devices = "/laboratories/" + plant.lab() + "/devices";
        var identity = "ENV-" + suffix();

        var spaceMonitor = call("POST", devices + "/environmental-devices", token, device("Space monitor", identity));
        assertThat(spaceMonitor.statusCode()).withFailMessage(spaceMonitor.body()).isEqualTo(201);
        long spaceMonitorId = id(spaceMonitor);
        assertThat(spaceMonitor.body()).contains("\"deviceType\":\"ENVIRONMENTAL_DEVICE\"").contains("\"sensorExternalId\":\"" + identity + "\"")
                .contains("\"firmwareVersion\":\"1.0.3\"");
        assertThat(spaceMonitor.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith(plant.equipment() + "/" + spaceMonitorId));
        assertThat(call("POST", devices + "/container-monitors", token, device("Duplicated identity", identity)).statusCode()).isEqualTo(409);

        long containerMonitor = id(call("POST", devices + "/container-monitors", token, device("Cold cabinet monitor", "CNT-" + suffix())));
        long secondContainer = id(call("POST", devices + "/container-monitors", token, device("Shelf monitor", "CNT-" + suffix())));
        assertThat(call("POST", devices + "/container-monitors", operatorOf(plant).token(), device("Operator", "CNT-" + suffix()))
                .statusCode()).isEqualTo(403);

        var environment = plant.environment(plant.storage());
        assertThat(call("POST", environment + "/environmental-devices", token, "{\"deviceId\":" + containerMonitor + "}").statusCode())
                .isEqualTo(404);
        var associated = call("POST", environment + "/environmental-devices", token, "{\"deviceId\":" + spaceMonitorId + "}");
        assertThat(associated.statusCode()).withFailMessage(associated.body()).isEqualTo(201);
        assertThat(associated.body()).contains("\"environmentId\":" + plant.storage());

        long secondSpaceMonitor = id(call("POST", devices + "/environmental-devices", token, device("Second space monitor", "ENV-" + suffix())));
        assertThat(call("POST", environment + "/environmental-devices", token, "{\"deviceId\":" + secondSpaceMonitor + "}").statusCode())
                .isEqualTo(409);
        assertThat(call("POST", environment + "/equipments", token, "{\"equipmentId\":" + secondSpaceMonitor + "}").statusCode())
                .isEqualTo(409);
        assertThat(call("POST", environment + "/environmental-devices", token, "{\"deviceId\":" + spaceMonitorId + "}").statusCode())
                .isEqualTo(201);

        assertThat(call("POST", environment + "/container-monitors", token, "{\"deviceId\":" + containerMonitor + "}").statusCode()).isEqualTo(201);
        assertThat(call("POST", environment + "/container-monitors", token, "{\"deviceId\":" + secondContainer + "}").statusCode()).isEqualTo(201);
        assertThat(call("POST", environment + "/container-monitors", token, "{\"deviceId\":" + spaceMonitorId + "}").statusCode()).isEqualTo(404);
        var foreign = plant();
        assertThat(call("POST", foreign.environment(foreign.storage()) + "/container-monitors", foreign.manager().token(),
                "{\"deviceId\":" + containerMonitor + "}").statusCode()).isEqualTo(404);

        var listed = call("GET", plant.equipment(), token, null);
        assertThat(JsonPath.<List<String>>read(listed.body(), "$[?(@.deviceType == 'CONTAINER_MONITOR')].name"))
                .containsExactlyInAnyOrder("Cold cabinet monitor", "Shelf monitor");
    }

    @Test
    void environmentListsOnlyItsOwnEquipmentAndIotDevices() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var devices = "/laboratories/" + plant.lab() + "/devices";
        var storage = plant.environment(plant.storage());
        assertThat(call("GET", storage + "/equipments", token, null).body()).isEqualTo("[]");

        long press = located(plant, plant.storage(), "Tablet press");
        long spaceMonitor = id(call("POST", devices + "/environmental-devices", token, device("Space monitor", "ENV-" + suffix())));
        long containerMonitor = id(call("POST", devices + "/container-monitors", token, device("Cold cabinet monitor", "CNT-" + suffix())));
        assertThat(call("POST", storage + "/environmental-devices", token, "{\"deviceId\":" + spaceMonitor + "}").statusCode()).isEqualTo(201);
        assertThat(call("POST", storage + "/container-monitors", token, "{\"deviceId\":" + containerMonitor + "}").statusCode()).isEqualTo(201);

        long production = environment(plant, "PROD-" + suffix());
        long productionMonitor = id(call("POST", devices + "/container-monitors", token, device("Production monitor", "CNT-" + suffix())));
        assertThat(call("POST", plant.environment(production) + "/container-monitors", token, "{\"deviceId\":" + productionMonitor + "}")
                .statusCode()).isEqualTo(201);
        id(call("POST", plant.equipment(), token, equipment("Unlocated mixer", "SN-" + suffix())));

        var listed = call("GET", storage + "/equipments", operatorOf(plant).token(), null);
        assertThat(listed.statusCode()).withFailMessage(listed.body()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(listed.body(), "$[*].name"))
                .containsExactly("Cold cabinet monitor", "Space monitor", "Tablet press");
        assertThat(JsonPath.<List<Integer>>read(listed.body(), "$[*].environmentId")).containsOnly((int) plant.storage());

        var nodes = call("GET", storage + "/equipments?iot=true", token, null);
        assertThat(nodes.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(nodes.body(), "$[*].id"))
                .containsExactlyInAnyOrder((int) spaceMonitor, (int) containerMonitor)
                .doesNotContain((int) press, (int) productionMonitor);
        assertThat(JsonPath.<List<String>>read(nodes.body(), "$[*].sensorExternalId")).doesNotContainNull();

        assertThat(JsonPath.<List<Integer>>read(call("GET", plant.environment(production) + "/equipments?iot=true", token, null).body(),
                "$[*].id")).containsExactly((int) productionMonitor);

        var foreign = plant();
        assertThat(call("GET", plant.environment(foreign.storage()) + "/equipments", token, null).statusCode()).isEqualTo(403);
        assertThat(call("GET", plant.environment(999999999L) + "/equipments", token, null).statusCode()).isEqualTo(403);
        assertThat(call("GET", foreign.environment(foreign.storage()) + "/equipments", token, null).statusCode()).isEqualTo(403);
    }

    @Test
    void deviceTelemetryStatusTellsWhetherTheDeviceIsCommunicating() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var devices = "/laboratories/" + plant.lab() + "/devices";
        long monitor = id(call("POST", devices + "/container-monitors", token, device("Cold cabinet monitor", "CNT-" + suffix())));
        var telemetryStatus = plant.environment(plant.storage()) + "/devices/" + monitor + "/telemetry-status";
        assertThat(call("GET", telemetryStatus, token, null).statusCode()).isEqualTo(404);

        assertThat(call("POST", plant.environment(plant.storage()) + "/container-monitors", token, "{\"deviceId\":" + monitor + "}")
                .statusCode()).isEqualTo(201);
        var silent = call("GET", telemetryStatus, operatorOf(plant).token(), null);
        assertThat(silent.statusCode()).withFailMessage(silent.body()).isEqualTo(200);
        assertThat(silent.body()).contains("\"connectionStatus\":\"REQUIRES_REVIEW\"").contains("\"lastCommunicationAt\":null")
                .contains("\"expectedPeriodSeconds\":300");

        var measurement = call("POST", plant.environment(plant.storage()) + "/container-monitors/" + monitor
                + "/telemetry-measurements", token, """
                {"metric":"TEMPERATURE","value":5.2,"measuredAt":"%s"}
                """.formatted(OffsetDateTime.now()));
        assertThat(measurement.statusCode()).withFailMessage(measurement.body()).isEqualTo(201);
        var connected = call("GET", telemetryStatus, token, null);
        assertThat(connected.body()).contains("\"connectionStatus\":\"CONNECTED\"");
        assertThat(JsonPath.<String>read(connected.body(), "$.lastCommunicationAt")).isNotNull();

        long press = located(plant, plant.storage(), "Tablet press");
        assertThat(call("GET", plant.environment(plant.storage()) + "/devices/" + press + "/telemetry-status", token, null)
                .statusCode()).isEqualTo(404);
        long otherEnvironment = environment(plant, "QC-" + suffix());
        assertThat(call("GET", plant.environment(otherEnvironment) + "/devices/" + monitor + "/telemetry-status", token, null)
                .statusCode()).isEqualTo(404);
    }

    @Test
    void edgeGatewayReadsOnlyTheFifteenDevicesItServes() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var storage = plant.environment(plant.storage());
        var gatewayIdentity = "EDGE-" + suffix();
        long gateway = edgeGateway(plant, gatewayIdentity);
        var apiKeyPath = "/laboratories/" + plant.lab() + "/edge-gateways/" + gateway + "/api-key";
        assertThat(call("GET", apiKeyPath, token, null).body()).contains("\"issued\":false");
        assertThat(call("POST", apiKeyPath, token, null).statusCode()).isEqualTo(409);

        assertThat(call("POST", storage + "/equipments", token, "{\"equipmentId\":" + gateway + "}").statusCode()).isEqualTo(201);
        var issued = call("POST", apiKeyPath, token, null);
        assertThat(issued.statusCode()).withFailMessage(issued.body()).isEqualTo(201);
        var apiKey = JsonPath.<String>read(issued.body(), "$.apiKey");
        var status = call("GET", apiKeyPath, token, null);
        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(status.body()).contains("\"issued\":true").doesNotContain(apiKey);
        assertThat(call("GET", apiKeyPath, operatorOf(plant).token(), null).statusCode()).isEqualTo(403);
        long press = located(plant, plant.storage(), "Tablet press");
        assertThat(call("GET", "/laboratories/" + plant.lab() + "/edge-gateways/" + press + "/api-key", token, null).statusCode())
                .isEqualTo(404);
        assertThat(call("GET", "/laboratories/" + plant.lab() + "/edge-gateways/999999999/api-key", token, null).statusCode())
                .isEqualTo(403);

        var monitors = containerMonitors(plant, plant.storage(), 20);
        long otherEnvironment = environment(plant, "QC-" + suffix());
        containerMonitors(plant, otherEnvironment, 1);

        var served = gatewayDevices(gatewayIdentity, apiKey);
        assertThat(served.statusCode()).withFailMessage(served.body()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(served.body(), "$[*].id"))
                .containsExactlyInAnyOrderElementsOf(monitors.subList(0, 15).stream().map(Long::intValue).toList());
        assertThat(JsonPath.<List<Integer>>read(served.body(), "$[*].environmentId")).containsOnly((int) plant.storage());
        assertThat(gatewayDevices(gatewayIdentity, "not-the-key").statusCode()).isEqualTo(401);

        var coverage = call("GET", storage + "/edge-gateway-coverage", operatorOf(plant).token(), null);
        assertThat(coverage.statusCode()).withFailMessage(coverage.body()).isEqualTo(200);
        assertThat(coverage.body()).contains("\"maxDevicesPerGateway\":15").contains("\"deviceCount\":20")
                .contains("\"uncoveredDeviceCount\":5");
        assertThat(JsonPath.<List<Integer>>read(coverage.body(), "$.gateways[*].equipmentId")).containsExactly((int) gateway);
        assertThat(JsonPath.<Integer>read(coverage.body(), "$.gateways[0].deviceCount")).isEqualTo(15);
        assertThat(JsonPath.<String>read(coverage.body(), "$.gateways[0].sensorExternalId")).isEqualTo(gatewayIdentity);

        var uncovered = call("GET", plant.environment(otherEnvironment) + "/edge-gateway-coverage", token, null);
        assertThat(uncovered.body()).contains("\"deviceCount\":1").contains("\"uncoveredDeviceCount\":1").contains("\"gateways\":[]");

        var foreign = plant();
        assertThat(call("GET", foreign.environment(foreign.storage()) + "/edge-gateway-coverage", token, null).statusCode())
                .isEqualTo(403);
        assertThat(call("GET", "/laboratories/" + foreign.lab() + "/edge-gateways/" + gateway + "/api-key", token, null)
                .statusCode()).isEqualTo(403);
    }

    @Test
    void secondEdgeGatewayOfTheEnvironmentServesTheDevicesLeftByTheFirst() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var storage = plant.environment(plant.storage());
        var monitors = containerMonitors(plant, plant.storage(), 20).stream().map(Long::intValue).toList();
        var firstIdentity = "EDGE-" + suffix();
        var secondIdentity = "EDGE-" + suffix();
        long first = edgeGateway(plant, firstIdentity);
        long second = edgeGateway(plant, secondIdentity);
        for (long gateway : List.of(first, second)) {
            var assigned = call("POST", storage + "/equipments", token, "{\"equipmentId\":" + gateway + "}");
            assertThat(assigned.statusCode()).withFailMessage(assigned.body()).isEqualTo(201);
        }

        var firstDevices = gatewayDevices(firstIdentity, apiKey(plant, first));
        var secondDevices = gatewayDevices(secondIdentity, apiKey(plant, second));
        assertThat(JsonPath.<List<Integer>>read(firstDevices.body(), "$[*].id"))
                .containsExactlyInAnyOrderElementsOf(monitors.subList(0, 15));
        assertThat(JsonPath.<List<Integer>>read(secondDevices.body(), "$[*].id"))
                .containsExactlyInAnyOrderElementsOf(monitors.subList(15, 20));

        var coverage = call("GET", storage + "/edge-gateway-coverage", token, null);
        assertThat(coverage.body()).contains("\"deviceCount\":20").contains("\"uncoveredDeviceCount\":0");
        assertThat(JsonPath.<List<Integer>>read(coverage.body(), "$.gateways[*].equipmentId"))
                .containsExactly((int) first, (int) second);
        assertThat(JsonPath.<List<Integer>>read(coverage.body(), "$.gateways[*].deviceCount")).containsExactly(15, 5);
    }

    @Test
    void edgeGatewaySynchronizesOnlyTheDevicesItServes() throws Exception {
        var plant = plant();
        var token = plant.manager().token();
        var storage = plant.environment(plant.storage());
        var monitors = containerMonitors(plant, plant.storage(), 16);
        var firstIdentity = "EDGE-" + suffix();
        var secondIdentity = "EDGE-" + suffix();
        long first = edgeGateway(plant, firstIdentity);
        long second = edgeGateway(plant, secondIdentity);
        for (long gateway : List.of(first, second)) {
            var assigned = call("POST", storage + "/equipments", token, "{\"equipmentId\":" + gateway + "}");
            assertThat(assigned.statusCode()).withFailMessage(assigned.body()).isEqualTo(201);
        }
        var firstKey = apiKey(plant, first);
        var secondKey = apiKey(plant, second);
        long servedByFirst = monitors.get(0);
        long servedBySecond = monitors.get(15);

        var own = gatewayMeasurement(firstIdentity, firstKey, servedByFirst);
        assertThat(own.statusCode()).withFailMessage(own.body()).isEqualTo(201);
        assertThat(gatewayMeasurement(secondIdentity, secondKey, servedBySecond).statusCode()).isEqualTo(201);
        // A device of the same environment that the other Gateway serves is refused.
        assertThat(gatewayMeasurement(secondIdentity, secondKey, servedByFirst).statusCode()).isEqualTo(403);
        assertThat(gatewayMeasurement(firstIdentity, firstKey, servedBySecond).statusCode()).isEqualTo(403);
        assertThat(gatewayProfile(secondIdentity, secondKey, servedByFirst).statusCode()).isEqualTo(403);

        // A Gateway out of service serves no device: the next operational one takes its devices.
        var outOfService = call("POST", storage + "/equipments/" + first + "/status-changes", token,
                "{\"status\":\"out_of_service\",\"reason\":\"Replaced by another Gateway\"}");
        assertThat(outOfService.statusCode()).withFailMessage(outOfService.body()).isEqualTo(201);
        assertThat(gatewayMeasurement(firstIdentity, firstKey, servedByFirst).statusCode()).isEqualTo(401);
        assertThat(gatewayMeasurement(secondIdentity, secondKey, servedByFirst).statusCode()).isEqualTo(201);
        var coverage = call("GET", storage + "/edge-gateway-coverage", token, null);
        assertThat(JsonPath.<List<Integer>>read(coverage.body(), "$.gateways[*].equipmentId"))
                .containsExactly((int) first, (int) second);
        assertThat(JsonPath.<List<Integer>>read(coverage.body(), "$.gateways[*].deviceCount")).containsExactly(0, 15);
        assertThat(coverage.body()).contains("\"uncoveredDeviceCount\":1");
    }

    private long edgeGateway(Plant plant, String identity) throws Exception {
        var registered = call("POST", "/laboratories/" + plant.lab() + "/edge-gateways", plant.manager().token(), """
                {"name":"Edge Gateway %1$s","sensorExternalId":"%1$s","serialNumber":"RPI-%2$s","model":"Raspberry Pi 4"}
                """.formatted(identity, suffix()));
        assertThat(registered.statusCode()).withFailMessage(registered.body()).isEqualTo(201);
        return id(registered);
    }

    private String apiKey(Plant plant, long gateway) throws Exception {
        var issued = call("POST", "/laboratories/" + plant.lab() + "/edge-gateways/" + gateway + "/api-key",
                plant.manager().token(), null);
        assertThat(issued.statusCode()).withFailMessage(issued.body()).isEqualTo(201);
        return JsonPath.read(issued.body(), "$.apiKey");
    }

    /** Registers container monitors and locates them in the environment; returns their ids in registration order. */
    private List<Long> containerMonitors(Plant plant, long environmentId, int count) throws Exception {
        var token = plant.manager().token();
        var ids = new ArrayList<Long>();
        for (int number = 1; number <= count; number++) {
            long monitor = id(call("POST", "/laboratories/" + plant.lab() + "/devices/container-monitors", token,
                    device("Container monitor " + number, "CNT-" + suffix())));
            var located = call("POST", plant.environment(environmentId) + "/container-monitors", token,
                    "{\"deviceId\":" + monitor + "}");
            assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
            ids.add(monitor);
        }
        return ids;
    }

    /** Reads the device registry the way the Edge does: with the Gateway identity and its API key. */
    private HttpResponse<String> gatewayDevices(String gatewayIdentity, String apiKey) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/edge-gateways/"
                + gatewayIdentity + "/iot-devices")).header("X-API-Key", apiKey).GET();
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Sends a reading of a device the way the Edge does: with the Gateway identity and its API key. */
    private HttpResponse<String> gatewayMeasurement(String gatewayIdentity, String apiKey, long deviceId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/edge-gateways/"
                        + gatewayIdentity + "/iot-devices/" + deviceId + "/telemetry-measurements"))
                .header("X-API-Key", apiKey).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"metric":"TEMPERATURE","value":5.2,"measuredAt":"%s"}
                        """.formatted(OffsetDateTime.now())));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Reads the profile of a device the way the Edge does. */
    private HttpResponse<String> gatewayProfile(String gatewayIdentity, String apiKey, long deviceId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/edge-gateways/"
                + gatewayIdentity + "/iot-devices/" + deviceId + "/environmental-profile")).header("X-API-Key", apiKey).GET();
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private long located(Plant plant, long environmentId, String name) throws Exception {
        var token = plant.manager().token();
        long equipmentId = id(call("POST", plant.equipment(), token, equipment(name, "SN-" + suffix())));
        var located = call("POST", plant.environment(environmentId) + "/equipments", token, "{\"equipmentId\":" + equipmentId + "}");
        assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
        return equipmentId;
    }

    private static String equipment(String name, String serialNumber) {
        return """
                {"name":"%s","type":"Tablet press","model":"TP-200","serialNumber":"%s"}
                """.formatted(name, serialNumber);
    }

    private static String device(String name, String identity) {
        return """
                {"name":"%s","sensorExternalId":"%s","serialNumber":"MAC-%s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(name, identity, suffix());
    }

    private static String maintenanceBody(LocalDate date, String type, long technicianStaffId) {
        return """
                {"maintenanceDate":"%s","technicianStaffId":%d,"description":"Routine check","type":"%s"}
                """.formatted(date, technicianStaffId, type);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Plant plant() throws Exception {
        var manager = account("ROLE_QA_MANAGER");
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Equipment laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        var storage = call("POST", "/laboratories/" + lab + "/environments", manager.token(),
                "{\"code\":\"WH-01\",\"name\":\"Raw material warehouse\"}");
        assertThat(storage.statusCode()).withFailMessage(storage.body()).isEqualTo(201);
        return new Plant(manager, lab, id(storage));
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
        var username = "equipment-" + UUID.randomUUID();
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
