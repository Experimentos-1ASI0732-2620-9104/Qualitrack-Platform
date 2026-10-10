package com.iotech.qualitrack.platform.unit.equipment;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.ConfigureBpmParametersCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterMaintenanceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.BpmParameterConfig;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.MaintenanceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EquipmentTests {
    @Test
    void registeredEquipmentIsOperationalWithoutSensor() {
        var equipment = newEquipment();

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(equipment.getType().name()).isEqualTo("REFRIGERATOR");
        assertThat(equipment.getSensorExternalId()).isNull();
    }

    @Test
    void registerCommandRequiresAllIdentificationData() {
        assertThatThrownBy(() -> new RegisterEquipmentCommand(0L, "Fridge", "REFRIGERATOR", "R-1", "SN-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisterEquipmentCommand(1L, " ", "REFRIGERATOR", "R-1", "SN-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisterEquipmentCommand(1L, "Fridge", "REFRIGERATOR", "R-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sensorLinkValidatesTheDeviceIdentifier() {
        var equipment = newEquipment();
        equipment.linkSensor("ESP32-TEMP-001");

        assertThat(equipment.getSensorExternalId()).isEqualTo(new DeviceId("ESP32-TEMP-001"));
        assertThatThrownBy(() -> equipment.linkSensor(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> equipment.linkSensor("X".repeat(51))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusUpdateRequiresAValue() {
        var equipment = newEquipment();
        equipment.updateStatus(EquipmentStatus.MAINTENANCE);

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
        assertThatThrownBy(() -> equipment.updateStatus(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void maintenanceRecordParsesDateAndTypeCaseInsensitively() {
        var record = new MaintenanceRecord(new RegisterMaintenanceCommand(1L, "2026-05-20", "John Smith",
                "Annual calibration", "calibration"));

        assertThat(record.getMaintenanceDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(record.getType()).isEqualTo(MaintenanceType.CALIBRATION);
    }

    @Test
    void maintenanceRecordRejectsInvalidDateOrType() {
        assertThatThrownBy(() -> new MaintenanceRecord(new RegisterMaintenanceCommand(1L, "20/05/2026", "John", "Work", "PREVENTIVE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("YYYY-MM-DD");
        assertThatThrownBy(() -> new MaintenanceRecord(new RegisterMaintenanceCommand(1L, "2026-05-20", "John", "Work", "CLEANING")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CLEANING");
    }

    @Test
    void bpmConfigurationRequiresAValidRange() {
        var config = new BpmParameterConfig(new ConfigureBpmParametersCommand(1L, "TEMPERATURE", 2.0, 8.0, "Celsius"));
        assertThat(config.getParameterName().name()).isEqualTo("TEMPERATURE");

        config.updateLimits(3.0, 7.5);
        assertThat(config.getMinValue()).isEqualTo(3.0);
        assertThat(config.getMaxValue()).isEqualTo(7.5);

        assertThatThrownBy(() -> config.updateLimits(8.0, 8.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.updateLimits(null, 8.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConfigureBpmParametersCommand(1L, "TEMPERATURE", 9.0, 8.0, "Celsius"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Equipment newEquipment() {
        return new Equipment(new RegisterEquipmentCommand(1L, "Cold storage A", "REFRIGERATOR", "R-200", "SN-0001"));
    }
}
