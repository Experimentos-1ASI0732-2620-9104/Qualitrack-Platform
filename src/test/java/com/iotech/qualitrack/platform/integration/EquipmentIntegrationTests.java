package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentIntegrationTests extends IntegrationTestSupport {
    @Test
    void registeredEquipmentIsPersistedAndListedForItsLaboratory() throws Exception {
        var owner = readyAccount();

        var created = registerEquipment(owner);
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long equipmentId = id(created);
        assertThat(json(created, "$.status").toString()).isEqualTo("OPERATIONAL");

        var found = call("GET", "/equipments/" + equipmentId, owner.token(), null);
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(json(found, "$.name").toString()).isEqualTo("Cold storage A");

        var listed = call("GET", "/equipments?labId=" + owner.laboratoryId(), owner.token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed, "$[*].id").toString()).contains(String.valueOf(equipmentId));
    }

    @Test
    void maintenanceRecordsAreStoredPerEquipment() throws Exception {
        var owner = readyAccount();
        long equipmentId = id(registerEquipment(owner));

        var maintenance = call("POST", "/equipments/" + equipmentId + "/maintenance-records", owner.token(), """
                {"maintenanceDate":"2026-05-20","technicianName":"John Smith",
                 "description":"Annual calibration","type":"CALIBRATION"}
                """);
        assertThat(maintenance.statusCode()).withFailMessage(maintenance.body()).isEqualTo(201);

        var history = call("GET", "/equipments/" + equipmentId + "/maintenance-records", owner.token(), null);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(json(history, "$.length()").toString()).isEqualTo("1");
        assertThat(json(history, "$[0].technicianName").toString()).isEqualTo("John Smith");
    }

    @Test
    void invalidMaintenanceAndBpmRangesAreRejectedWithoutPersisting() throws Exception {
        var owner = readyAccount();
        long equipmentId = id(registerEquipment(owner));

        var badMaintenance = call("POST", "/equipments/" + equipmentId + "/maintenance-records", owner.token(), """
                {"maintenanceDate":"20/05/2026","technicianName":"John","description":"Work","type":"PREVENTIVE"}
                """);
        var badRange = call("POST", "/equipments/" + equipmentId + "/bpm-configs", owner.token(), """
                {"parameterName":"TEMPERATURE","minValue":9.0,"maxValue":8.0,"unit":"Celsius"}
                """);

        assertThat(badMaintenance.statusCode()).isEqualTo(400);
        assertThat(badRange.statusCode()).isEqualTo(400);
        assertThat(call("GET", "/equipments/" + equipmentId + "/maintenance-records", owner.token(), null).body()).isEqualTo("[]");
        assertThat(call("GET", "/equipments/" + equipmentId + "/bpm-configs", owner.token(), null).body()).isEqualTo("[]");
    }

    @Test
    void anotherLaboratoryCannotReadOrRegisterEquipment() throws Exception {
        var owner = readyAccount();
        var intruder = readyAccount();
        long equipmentId = id(registerEquipment(owner));

        assertThat(call("GET", "/equipments/" + equipmentId, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", "/equipments?labId=" + owner.laboratoryId(), intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(registerEquipment(intruder, owner.laboratoryId()).statusCode()).isEqualTo(403);
    }

    private java.net.http.HttpResponse<String> registerEquipment(ReadyAccount owner) throws Exception {
        return registerEquipment(owner, owner.laboratoryId());
    }

    private java.net.http.HttpResponse<String> registerEquipment(ReadyAccount actor, long laboratoryId) throws Exception {
        return call("POST", "/equipments", actor.token(), """
                {"laboratoryId":%d,"name":"Cold storage A","type":"REFRIGERATOR","model":"R-200","serialNumber":"SN-%s"}
                """.formatted(laboratoryId, UUID.randomUUID()));
    }
}
