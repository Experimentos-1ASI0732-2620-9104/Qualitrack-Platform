package com.iotech.qualitrack.platform.domain.model.commands;

import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateDeviationAlertCommandTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-10-04T14:05:00Z");

    private static CreateDeviationAlertCommand command(Long laboratoryId, Long environmentId, Long deviceId,
                                                       String parameterName, Double recordedValue, Double thresholdValue,
                                                       String unit, AlertSeverity severity, Instant detectedAt) {
        return new CreateDeviationAlertCommand(laboratoryId, environmentId, deviceId, null, parameterName, recordedValue,
                thresholdValue, unit, severity, detectedAt);
    }

    @Test
    void normalizesParameterNameAndUnit() {
        var command = command(1L, 2L, 3L, "  temperature ", 33.0, 30.0, " °C ", AlertSeverity.CRITICAL, DETECTED_AT);

        assertThat(command.parameterName()).isEqualTo("TEMPERATURE");
        assertThat(command.unit()).isEqualTo("°C");
    }

    @Test
    void acceptsAMissingDeviceToUseTheEnvironmentalDevice() {
        var command = command(1L, 2L, null, "HUMIDITY", 80.0, 70.0, "%", AlertSeverity.WARNING, DETECTED_AT);

        assertThat(command.deviceId()).isNull();
    }

    @Test
    void rejectsMissingOrInvalidIdentifiers() {
        assertThatThrownBy(() -> command(null, 2L, 3L, "TEMPERATURE", 33.0, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("laboratoryId");
        assertThatThrownBy(() -> command(1L, 0L, 3L, "TEMPERATURE", 33.0, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("environmentId");
        assertThatThrownBy(() -> command(1L, 2L, -5L, "TEMPERATURE", 33.0, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("deviceId");
    }

    @Test
    void rejectsValuesThatAreNotNumbers() {
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", Double.NaN, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recordedValue");
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", 33.0, Double.POSITIVE_INFINITY, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("thresholdValue");
    }

    @Test
    void rejectsBlankTextsAndMissingSeverityOrDate() {
        assertThatThrownBy(() -> command(1L, 2L, 3L, " ", 33.0, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parameterName");
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", 33.0, 30.0, "", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unit");
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", 33.0, 30.0, "°C", null, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("severity");
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", 33.0, 30.0, "°C", AlertSeverity.CRITICAL, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("detectedAt");
    }

    @Test
    void rejectsTextsLongerThanTheirColumns() {
        var longName = "T".repeat(CreateDeviationAlertCommand.MAX_PARAMETER_NAME_LENGTH + 1);
        var longUnit = "u".repeat(CreateDeviationAlertCommand.MAX_UNIT_LENGTH + 1);

        assertThatThrownBy(() -> command(1L, 2L, 3L, longName, 33.0, 30.0, "°C", AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parameterName");
        assertThatThrownBy(() -> command(1L, 2L, 3L, "TEMPERATURE", 33.0, 30.0, longUnit, AlertSeverity.CRITICAL, DETECTED_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unit");
    }

    @Test
    void measuresTheLengthAfterTrimming() {
        var unit = "  " + "u".repeat(CreateDeviationAlertCommand.MAX_UNIT_LENGTH) + "  ";

        assertThat(command(1L, 2L, 3L, "TEMPERATURE", 33.0, 30.0, unit, AlertSeverity.CRITICAL, DETECTED_AT).unit())
                .hasSize(CreateDeviationAlertCommand.MAX_UNIT_LENGTH);
    }
}
