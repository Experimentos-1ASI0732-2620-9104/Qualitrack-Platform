package com.iotech.qualitrack.platform.unit.ca;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DeviationAlertTests {
    private static final String TIMESTAMP = "2026-05-12T11:00:00Z";

    @Test
    void newAlertIsUnresolvedAndKeepsTheMeasuredDeviation() {
        var alert = newAlert(AlertSeverity.CRITICAL);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UNRESOLVED);
        assertThat(alert.isUnresolved()).isTrue();
        assertThat(alert.isCritical()).isTrue();
        assertThat(alert.getRecordedValue()).isEqualTo(9.5);
        assertThat(alert.getThresholdValue()).isEqualTo(8.0);
        assertThat(alert.getAcknowledgedBy()).isNull();
    }

    @Test
    void createCommandRejectsIncompleteDeviationData() {
        assertThatThrownBy(() -> new CreateDeviationAlertCommand(0L, null, "TEMPERATURE", 9.5, 8.0, "Celsius", TIMESTAMP, AlertSeverity.CRITICAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateDeviationAlertCommand(1L, 0L, "TEMPERATURE", 9.5, 8.0, "Celsius", TIMESTAMP, AlertSeverity.CRITICAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateDeviationAlertCommand(1L, null, "TEMPERATURE", null, 8.0, "Celsius", TIMESTAMP, AlertSeverity.CRITICAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateDeviationAlertCommand(1L, null, "TEMPERATURE", 9.5, 8.0, "Celsius", TIMESTAMP, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acknowledgementRecordsTheUserAndCannotBeRepeated() {
        var alert = newAlert(AlertSeverity.WARNING);
        alert.acknowledge(new AcknowledgeAlertCommand(1L, 10L));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedBy()).isEqualTo(10L);
        assertThat(alert.isUnresolved()).isFalse();
        assertThat(alert.isCritical()).isFalse();
        assertThatThrownBy(() -> alert.acknowledge(new AcknowledgeAlertCommand(1L, 11L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolutionStoresCorrectiveActionAndClosesTheAlert() {
        var alert = newAlert(AlertSeverity.CRITICAL);
        alert.acknowledge(new AcknowledgeAlertCommand(1L, 10L));
        alert.resolve(new ResolveAlertCommand(1L, 12L, "Equipment recalibrated"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedBy()).isEqualTo(12L);
        assertThat(alert.getResolutionNotes()).isEqualTo("Equipment recalibrated");
        assertThatThrownBy(() -> alert.resolve(new ResolveAlertCommand(1L, 12L, "Again")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> alert.acknowledge(new AcknowledgeAlertCommand(1L, 10L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unacknowledgedAlertCanBeResolvedDirectlyButNeedsNotes() {
        var alert = newAlert(AlertSeverity.CRITICAL);
        assertThatThrownBy(() -> new ResolveAlertCommand(1L, 12L, " "))
                .isInstanceOf(IllegalArgumentException.class);

        alert.resolve(new ResolveAlertCommand(1L, 12L, "Door closed and temperature recovered"));
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    private DeviationAlert newAlert(AlertSeverity severity) {
        return new DeviationAlert(new CreateDeviationAlertCommand(1L, 2L, "TEMPERATURE", 9.5, 8.0,
                "Celsius", TIMESTAMP, severity));
    }
}
