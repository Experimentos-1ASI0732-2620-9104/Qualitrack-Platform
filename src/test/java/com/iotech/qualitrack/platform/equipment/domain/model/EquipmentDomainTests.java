package com.iotech.qualitrack.platform.equipment.domain.model;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterIotDeviceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentDomainTests {

    @Test
    void iotDeviceIsAnEquipmentWithTypeAndIdentity() {
        var device = new Equipment(new RegisterIotDeviceCommand(1L, IotDeviceType.CONTAINER_MONITOR, " Cold cabinet monitor ",
                " CNT-001 ", "24:6F:28:AA:10:01", "ESP32-WROOM-32", " "));

        assertThat(device.isIotDevice()).isTrue();
        assertThat(device.isDeviceOfType(IotDeviceType.CONTAINER_MONITOR)).isTrue();
        assertThat(device.isDeviceOfType(IotDeviceType.ENVIRONMENTAL_DEVICE)).isFalse();
        assertThat(device.getName()).isEqualTo("Cold cabinet monitor");
        assertThat(device.getSensorExternalId().value()).isEqualTo("CNT-001");
        assertThat(device.getFirmwareVersion()).isNull();
        assertThat(device.getStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(device.getEnvironmentId()).isNull();

        var press = new Equipment(new RegisterEquipmentCommand(1L, "Tablet press", "Press", "TP-200", "SN-1"));
        assertThat(press.isIotDevice()).isFalse();
        assertThatThrownBy(() -> new RegisterIotDeviceCommand(1L, null, "Name", "ID", "SN", "Model", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisterIotDeviceCommand(1L, IotDeviceType.ENVIRONMENTAL_DEVICE, "Name", "X".repeat(51), "SN", "Model", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equipmentIsLocatedOnlyInEnvironmentsOfItsLaboratory() {
        var press = new Equipment(new RegisterEquipmentCommand(1L, "Tablet press", "Press", "TP-200", "SN-1"));
        assertThat(press.isLocatedIn(1L, 5L)).isFalse();

        press.assignToEnvironment(5L);

        assertThat(press.isLocatedIn(1L, 5L)).isTrue();
        assertThat(press.isLocatedIn(2L, 5L)).isFalse();
        assertThat(press.isLocatedIn(1L, 6L)).isFalse();
        assertThatThrownBy(() -> press.assignToEnvironment(0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusChangeKeepsTheHistoryAndRejectsTheCurrentStatus() {
        var press = new Equipment(new RegisterEquipmentCommand(1L, "Tablet press", "Press", "TP-200", "SN-1"));
        press.assignToEnvironment(5L);
        var at = Instant.parse("2026-10-03T15:00:00Z");

        var change = press.changeStatus(EquipmentStatus.OUT_OF_SERVICE, "Broken belt", 7L, at);

        assertThat(change.getPreviousStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(change.getNewStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
        assertThat(change.getEnvironmentId()).isEqualTo(5L);
        assertThat(change.getChangedByUserId()).isEqualTo(7L);
        assertThat(press.getStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
        assertThatThrownBy(() -> press.changeStatus(EquipmentStatus.OUT_OF_SERVICE, null, 7L, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(press.getStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
    }
}
