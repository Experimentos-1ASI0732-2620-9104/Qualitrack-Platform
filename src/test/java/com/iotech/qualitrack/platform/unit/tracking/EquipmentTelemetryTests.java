package com.iotech.qualitrack.platform.unit.tracking;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EquipmentTelemetry;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordMeasurementCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.EquipmentTelemetryStatus;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.TelemetryStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EquipmentTelemetryTests {
    private static final String TIMESTAMP = "2026-05-12T10:00:00Z";

    @Test
    void newTelemetryStartsOfflineAndEmpty() {
        var telemetry = EquipmentTelemetry.createForEquipment(7L);

        assertThat(telemetry.isOnline()).isFalse();
        assertThat(telemetry.getStatus().getCurrentStatus()).isEqualTo(TelemetryStatus.OFFLINE);
        assertThat(telemetry.getMeasurements()).isEmpty();
        assertThat(telemetry.hasAnomalies()).isFalse();
        assertThatThrownBy(() -> EquipmentTelemetry.createForEquipment(0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void measurementsOnlyAcceptReadingsFromTheSameEquipment() {
        var telemetry = EquipmentTelemetry.createForEquipment(7L);
        telemetry.recordMeasurement(Measurement.record(7L, "TEMPERATURE", 4.2, "Celsius", TIMESTAMP));

        assertThat(telemetry.getMeasurements()).singleElement().extracting(Measurement::getValue).isEqualTo(4.2);
        assertThatThrownBy(() -> telemetry.recordMeasurement(Measurement.record(8L, "TEMPERATURE", 4.2, "Celsius", TIMESTAMP)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> telemetry.getMeasurements().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void statusUpdateMarksEquipmentOnlineAndRejectsForeignStatus() {
        var telemetry = EquipmentTelemetry.createForEquipment(7L);
        telemetry.updateStatus(EquipmentTelemetryStatus.update(7L, true, TelemetryStatus.OPERATIONAL, TIMESTAMP));

        assertThat(telemetry.isOnline()).isTrue();
        assertThatThrownBy(() -> telemetry.updateStatus(EquipmentTelemetryStatus.update(8L, true, TelemetryStatus.WARNING, TIMESTAMP)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void measurementCommandRequiresCompleteReading() {
        assertThatThrownBy(() -> new RecordMeasurementCommand(7L, "TEMPERATURE", null, "Celsius", TIMESTAMP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecordMeasurementCommand(7L, "TEMPERATURE", 4.2, " ", TIMESTAMP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecordMeasurementCommand(-1L, "TEMPERATURE", 4.2, "Celsius", TIMESTAMP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
