package com.iotech.qualitrack.platform.unit.tracking;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordMeasurementCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class EquipmentTelemetryTests {
    private static final Instant MEASURED_AT = Instant.parse("2026-05-12T10:00:00Z");

    @Test
    void newProfileStartsAtVersionZeroAndEmpty() {
        var profile = EnvironmentalProfile.forContainerMonitor(1L, 7L);

        assertThat(profile.getVersion()).isZero();
        assertThat(profile.getDeviceId()).isEqualTo(7L);
        assertThat(profile.getThresholds()).isEmpty();
        assertThat(profile.getActuationRules()).isEmpty();
        assertThat(profile.evaluate(MonitoredMetric.TEMPERATURE, 4.2)).isEmpty();
        assertThatThrownBy(() -> EnvironmentalProfile.forContainerMonitor(1L, 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void profileOnlyAcceptsMetricsReportedByItsOwnDevice() {
        var profile = EnvironmentalProfile.forContainerMonitor(1L, 7L);
        profile.replaceThresholds(List.of(temperatureThreshold()), 3L, MEASURED_AT);

        assertThat(profile.getThresholds()).singleElement().extracting(EnvironmentalThreshold::metric)
                .isEqualTo(MonitoredMetric.TEMPERATURE);
        assertThatThrownBy(() -> profile.replaceThresholds(
                List.of(new EnvironmentalThreshold(MonitoredMetric.AIR_QUALITY, 0.0, 50.0, -1.0, 80.0)), 3L, MEASURED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.getThresholds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void thresholdUpdateIncreasesTheVersionAndEvaluatesReadings() {
        var profile = EnvironmentalProfile.forContainerMonitor(1L, 7L);
        profile.replaceThresholds(List.of(temperatureThreshold()), 3L, MEASURED_AT);

        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getUpdatedBy()).isEqualTo(3L);
        assertThat(profile.evaluate(MonitoredMetric.TEMPERATURE, 4.2)).get()
                .extracting(evaluation -> evaluation.state()).isEqualTo(EnvironmentalState.NORMAL);
        assertThat(profile.evaluate(MonitoredMetric.TEMPERATURE, 9.0)).get()
                .extracting(evaluation -> evaluation.state()).isEqualTo(EnvironmentalState.WARNING);
        assertThatThrownBy(() -> profile.replaceThresholds(List.of(temperatureThreshold(), temperatureThreshold()), 3L, MEASURED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void measurementCommandRequiresCompleteReading() {
        assertThatThrownBy(() -> new RecordMeasurementCommand(1L, 2L, 7L, null, 4.2, null, MEASURED_AT, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecordMeasurementCommand(1L, 2L, 7L, MonitoredMetric.TEMPERATURE, 4.2, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecordMeasurementCommand(-1L, 2L, 7L, MonitoredMetric.TEMPERATURE, 4.2, null, MEASURED_AT, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Measurement.receive(1L, 2L, 7L, MonitoredMetric.TEMPERATURE, null, null, MEASURED_AT, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private EnvironmentalThreshold temperatureThreshold() {
        return new EnvironmentalThreshold(MonitoredMetric.TEMPERATURE, 2.0, 8.0, 0.0, 10.0);
    }
}
