package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentIntegrationTests extends IntegrationTestSupport {
    @Test
    void registeredEquipmentIsPersistedAndListedForItsLaboratory() throws Exception {
        var owner = readyAccount();
        var equipments = "/laboratories/" + owner.laboratoryId() + "/equipments";

        var created = registerEquipment(owner, owner.laboratoryId(), "Cold storage A");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long equipmentId = id(created);
        assertThat(json(created, "$.status").toString()).isEqualTo("OPERATIONAL");

        var found = call("GET", equipments + "/" + equipmentId, owner.token(), null);
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(json(found, "$.name").toString()).isEqualTo("Cold storage A");

        var listed = call("GET", equipments, owner.token(), null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed, "$[*].id").toString()).contains(String.valueOf(equipmentId));
    }

    @Test
    void maintenanceRecordsAreStoredPerEquipment() throws Exception {
        var owner = readyAccount();
        long equipmentId = locatedEquipment(owner, "Cold storage A");
        var technician = staff(owner, "John Smith", "OPERATOR");
        var maintenanceRecords = environmentPath(owner) + "/equipments/" + equipmentId + "/maintenance-records";

        var maintenance = call("POST", maintenanceRecords, owner.token(), """
                {"maintenanceDate":"2026-05-20","technicianStaffId":%d,
                 "description":"Annual calibration","type":"CALIBRATION"}
                """.formatted(technician.staffId()));
        assertThat(maintenance.statusCode()).withFailMessage(maintenance.body()).isEqualTo(201);

        var history = call("GET", maintenanceRecords, owner.token(), null);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(json(history, "$.length()").toString()).isEqualTo("1");
        assertThat(json(history, "$[0].technicianName").toString()).isEqualTo("John Smith");
    }

    @Test
    void invalidMaintenanceAndBpmRangesAreRejectedWithoutPersisting() throws Exception {
        var owner = readyAccount();
        long equipmentId = locatedEquipment(owner, "Cold storage A");
        var technician = staff(owner, "John Smith", "OPERATOR");
        var maintenanceRecords = environmentPath(owner) + "/equipments/" + equipmentId + "/maintenance-records";
        var bpmConfigs = "/laboratories/" + owner.laboratoryId() + "/equipments/" + equipmentId + "/bpm-configs";

        var badMaintenance = call("POST", maintenanceRecords, owner.token(), """
                {"maintenanceDate":"20/05/2026","technicianStaffId":%d,"description":"Work","type":"PREVENTIVE"}
                """.formatted(technician.staffId()));
        var badRange = call("PUT", bpmConfigs + "/TEMPERATURE", owner.token(), """
                {"minValue":9.0,"maxValue":8.0,"unit":"Celsius"}
                """);

        assertThat(badMaintenance.statusCode()).isEqualTo(400);
        assertThat(badRange.statusCode()).isEqualTo(400);
        assertThat(call("GET", maintenanceRecords, owner.token(), null).body()).isEqualTo("[]");
        assertThat(call("GET", bpmConfigs, owner.token(), null).body()).isEqualTo("[]");
    }

    @Test
    void anotherLaboratoryCannotReadOrRegisterEquipment() throws Exception {
        var owner = readyAccount();
        var intruder = readyAccount();
        long equipmentId = id(registerEquipment(owner, owner.laboratoryId(), "Cold storage A"));
        var equipments = "/laboratories/" + owner.laboratoryId() + "/equipments";

        assertThat(call("GET", equipments + "/" + equipmentId, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", equipments, intruder.token(), null).statusCode()).isEqualTo(403);
        assertThat(registerEquipment(intruder, owner.laboratoryId(), "Intruder equipment").statusCode()).isEqualTo(403);
    }
}
