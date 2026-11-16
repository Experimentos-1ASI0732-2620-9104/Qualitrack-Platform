package com.iotech.qualitrack.platform.domain.model.commands;

import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.RecordConditionNormalizedCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertLifecycleCommandsTest {

    @Test
    void acknowledgeRequiresAlertAndUser() {
        assertThat(new AcknowledgeAlertCommand(1L, 7L).acknowledgedBy()).isEqualTo(7L);
        assertThatThrownBy(() -> new AcknowledgeAlertCommand(null, 7L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AcknowledgeAlertCommand(1L, 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveRequiresNotes() {
        assertThat(new ResolveAlertCommand(1L, 7L, "Recalibrated").resolutionNotes()).isEqualTo("Recalibrated");
        assertThatThrownBy(() -> new ResolveAlertCommand(1L, 7L, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolveAlertCommand(1L, 7L, "   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolveAlertCommand(1L, -1L, "Recalibrated")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conditionNormalizedUppercasesTheParameter() {
        var command = new RecordConditionNormalizedCommand(1L, 3L, " humidity ", Instant.parse("2026-10-04T15:00:00Z"));

        assertThat(command.parameterName()).isEqualTo("HUMIDITY");
    }

    @Test
    void conditionNormalizedRequiresDeviceAndDate() {
        assertThatThrownBy(() -> new RecordConditionNormalizedCommand(1L, null, "HUMIDITY", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecordConditionNormalizedCommand(1L, 3L, "HUMIDITY", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveAcceptsNotesUpToTheColumnSize() {
        var notes = "x".repeat(ResolveAlertCommand.MAX_RESOLUTION_NOTES_LENGTH);

        assertThat(new ResolveAlertCommand(1L, 7L, notes).resolutionNotes()).hasSize(500);
    }

    @Test
    void resolveRejectsNotesLongerThanTheColumn() {
        var notes = "x".repeat(ResolveAlertCommand.MAX_RESOLUTION_NOTES_LENGTH + 1);

        assertThatThrownBy(() -> new ResolveAlertCommand(1L, 7L, notes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("500");
    }
}
