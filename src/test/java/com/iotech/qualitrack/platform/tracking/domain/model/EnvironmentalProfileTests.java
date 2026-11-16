package com.iotech.qualitrack.platform.tracking.domain.model;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState.CRITICAL;
import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState.NORMAL;
import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState.WARNING;
import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.AIR_QUALITY;
import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.HUMIDITY;
import static com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.TEMPERATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rules of the environmental profiles: thresholds, states, actuation rules and versions.
 */
class EnvironmentalProfileTests {
    private static final Instant NOW = Instant.parse("2026-10-03T15:00:00Z");

    @Test
    void aValueIsNormalInsideTheNormalRangeWarningUpToTheCriticalLimitsAndCriticalBeyond() {
        var temperature = new EnvironmentalThreshold(TEMPERATURE, 15.0, 25.0, 8.0, 30.0);

        assertThat(temperature.evaluate(20).state()).isEqualTo(NORMAL);
        assertThat(temperature.evaluate(25).state()).isEqualTo(NORMAL);
        assertThat(temperature.evaluate(27)).satisfies(result -> {
            assertThat(result.state()).isEqualTo(WARNING);
            assertThat(result.exceededLimit()).isEqualTo(25.0);
        });
        assertThat(temperature.evaluate(12).exceededLimit()).isEqualTo(15.0);
        assertThat(temperature.evaluate(30).state()).isEqualTo(WARNING);
        assertThat(temperature.evaluate(31)).satisfies(result -> {
            assertThat(result.state()).isEqualTo(CRITICAL);
            assertThat(result.exceededLimit()).isEqualTo(30.0);
        });
        assertThat(temperature.evaluate(5).exceededLimit()).isEqualTo(8.0);

        var airQuality = new EnvironmentalThreshold(AIR_QUALITY, null, 400.0, null, 800.0);
        assertThat(airQuality.evaluate(0).state()).isEqualTo(NORMAL);
        assertThat(airQuality.evaluate(900).state()).isEqualTo(CRITICAL);
    }

    @Test
    void contradictoryOrIncompleteLimitsAreRejected() {
        assertThatThrownBy(() -> new EnvironmentalThreshold(TEMPERATURE, 25.0, 15.0, 8.0, 30.0))
                .hasMessageContaining("normal minimum");
        assertThatThrownBy(() -> new EnvironmentalThreshold(TEMPERATURE, 15.0, 25.0, 16.0, 30.0))
                .hasMessageContaining("critical minimum");
        assertThatThrownBy(() -> new EnvironmentalThreshold(TEMPERATURE, 15.0, 25.0, 8.0, 24.0))
                .hasMessageContaining("critical maximum");
        assertThatThrownBy(() -> new EnvironmentalThreshold(HUMIDITY, 40.0, 60.0, null, 70.0))
                .hasMessageContaining("both its normal and its critical value");
        assertThatThrownBy(() -> new EnvironmentalThreshold(HUMIDITY, null, null, null, null))
                .hasMessageContaining("At least one limit");
        assertThatThrownBy(() -> new EnvironmentalThreshold(MonitoredMetric.MOTION, null, 1.0, null, 2.0))
                .hasMessageContaining("cannot be configured");
        assertThatThrownBy(() -> new EnvironmentalThreshold(TEMPERATURE, Double.NaN, 25.0, 8.0, 30.0))
                .hasMessageContaining("finite");
    }

    @Test
    void anEnvironmentOnlyConfiguresTheMetricsOfItsEnvironmentalDevice() {
        var profile = EnvironmentalProfile.forEnvironment(1L, 2L);
        profile.replaceThresholds(List.of(new EnvironmentalThreshold(AIR_QUALITY, null, 400.0, null, 800.0)), 9L, NOW);

        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getUpdatedBy()).isEqualTo(9L);
        assertThat(profile.evaluate(AIR_QUALITY, 500)).hasValueSatisfying(result -> assertThat(result.state()).isEqualTo(WARNING));
        assertThat(profile.evaluate(TEMPERATURE, 50)).isEmpty();
        assertThatThrownBy(() -> profile.replaceThresholds(
                List.of(new EnvironmentalThreshold(TEMPERATURE, 15.0, 25.0, 8.0, 30.0)), 9L, NOW))
                .hasMessageContaining("environmental device");
        assertThatThrownBy(() -> profile.replaceActuationRules(List.of(), 9L, NOW))
                .hasMessageContaining("container monitors");
        assertThat(profile.getVersion()).isEqualTo(1);
    }

    @Test
    void aRuleNeedsTheThresholdOfItsMetricAndEachChangeIncreasesTheVersion() {
        var profile = EnvironmentalProfile.forContainerMonitor(1L, 7L);
        var temperature = new EnvironmentalThreshold(TEMPERATURE, 15.0, 25.0, 8.0, 30.0);
        var ventilate = new ActuationRule(TEMPERATURE, WARNING, ActuationAction.VENTILATION_ON);

        assertThatThrownBy(() -> profile.replaceActuationRules(List.of(ventilate), 9L, NOW))
                .hasMessageContaining("needs a threshold");
        profile.replaceThresholds(List.of(temperature), 9L, NOW);
        profile.replaceActuationRules(List.of(ventilate, new ActuationRule(TEMPERATURE, CRITICAL, ActuationAction.COOLING_ON)), 9L, NOW);
        assertThat(profile.getVersion()).isEqualTo(2);

        assertThatThrownBy(() -> profile.replaceThresholds(
                List.of(new EnvironmentalThreshold(HUMIDITY, 40.0, 60.0, 30.0, 70.0)), 9L, NOW))
                .hasMessageContaining("needs a threshold");
        assertThatThrownBy(() -> profile.replaceThresholds(List.of(temperature, temperature), 9L, NOW))
                .hasMessageContaining("Only one threshold");
        assertThatThrownBy(() -> profile.replaceActuationRules(List.of(ventilate, ventilate), 9L, NOW))
                .hasMessageContaining("repeated");
        assertThatThrownBy(() -> new ActuationRule(TEMPERATURE, NORMAL, ActuationAction.VENTILATION_ON))
                .hasMessageContaining("WARNING or CRITICAL");
        assertThatThrownBy(() -> new ActuationRule(TEMPERATURE, WARNING, ActuationAction.VENTILATION_OFF))
                .hasMessageContaining("must start an action");
        assertThat(profile.getVersion()).isEqualTo(2);
    }

    @Test
    void onlyAWorseConditionIsANewDeviation() {
        assertThat(WARNING.worsens(null)).isTrue();
        assertThat(WARNING.worsens(NORMAL)).isTrue();
        assertThat(CRITICAL.worsens(WARNING)).isTrue();
        assertThat(WARNING.worsens(WARNING)).isFalse();
        assertThat(WARNING.worsens(CRITICAL)).isFalse();
        assertThat(NORMAL.worsens(CRITICAL)).isFalse();
    }

    @Test
    void aReadingMatchesItsMetric() {
        var rfid = Measurement.receive(1L, 2L, 7L, MonitoredMetric.RFID_TAG, null, " E200-01 ", NOW, null, null);
        assertThat(rfid.getTextValue()).isEqualTo("E200-01");
        assertThat(rfid.getUnit()).isEqualTo("tag");
        assertThatThrownBy(() -> Measurement.receive(1L, 2L, 7L, MonitoredMetric.RFID_TAG, 3.0, null, NOW, null, null))
                .hasMessageContaining("text value");
        assertThatThrownBy(() -> Measurement.receive(1L, 2L, 7L, MonitoredMetric.MOTION, 2.0, null, NOW, null, null))
                .hasMessageContaining("1 when detected");
        assertThatThrownBy(() -> Measurement.receive(1L, 2L, 7L, TEMPERATURE, null, null, NOW, null, null))
                .hasMessageContaining("numeric value");
        assertThat(Measurement.receive(1L, 2L, 7L, TEMPERATURE, 21.5, null, NOW, null, null).getState()).isNull();
        assertThat(EnvironmentalState.valueOf("CRITICAL")).isEqualTo(CRITICAL);
    }
}
