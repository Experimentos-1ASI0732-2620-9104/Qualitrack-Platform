package com.iotech.qualitrack.platform.laboratory.domain.model;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.UpdateEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentDomainTests {

    @Test
    void registrationNormalizesCodeAndStartsWithoutUsage() {
        var environment = new Environment(new RegisterEnvironmentCommand(1L, "  wh-rm-01 ", " Raw material warehouse ", "  "));

        assertThat(environment.getCode()).isEqualTo("WH-RM-01");
        assertThat(environment.getName()).isEqualTo("Raw material warehouse");
        assertThat(environment.getDescription()).isNull();
        assertThat(environment.getUsage()).isNull();
        assertThat(environment.belongsTo(1L)).isTrue();
        assertThat(environment.belongsTo(2L)).isFalse();
    }

    @Test
    void registrationRejectsMissingOrOversizedData() {
        assertThatThrownBy(() -> new RegisterEnvironmentCommand(1L, " ", "Name", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisterEnvironmentCommand(0L, "ENV", "Name", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Environment(new RegisterEnvironmentCommand(1L, "X".repeat(31), "Name", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Environment(new RegisterEnvironmentCommand(1L, "ENV", "N".repeat(101), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usageCanChangeButNotBeReassignedToTheSameValue() {
        var environment = new Environment(new RegisterEnvironmentCommand(1L, "PROD-1", "Production area", null));
        var assignedAt = Instant.parse("2026-10-02T10:00:00Z");

        environment.assignUsage(EnvironmentUsage.PRODUCTION, 7L, assignedAt);
        assertThat(environment.getUsage()).isEqualTo(EnvironmentUsage.PRODUCTION);
        assertThat(environment.getUsageAssignedBy()).isEqualTo(7L);
        assertThat(environment.getUsageAssignedAt()).isEqualTo(assignedAt);

        assertThatThrownBy(() -> environment.assignUsage(EnvironmentUsage.PRODUCTION, 7L, assignedAt))
                .isInstanceOf(IllegalStateException.class);

        environment.assignUsage(EnvironmentUsage.LABORATORY, 8L, assignedAt.plusSeconds(60));
        assertThat(environment.getUsage()).isEqualTo(EnvironmentUsage.LABORATORY);
        assertThat(environment.getUsageAssignedBy()).isEqualTo(8L);
    }

    @Test
    void updateReplacesIdentificationAndKeepsUsage() {
        var environment = new Environment(3L, 1L, "OLD", "Old name", "Old", EnvironmentUsage.OTHER, 7L,
                Instant.parse("2026-10-01T10:00:00Z"));

        environment.update(new UpdateEnvironmentCommand(1L, 3L, "new-code", "New name", "New description"));

        assertThat(environment.getCode()).isEqualTo("NEW-CODE");
        assertThat(environment.getName()).isEqualTo("New name");
        assertThat(environment.getDescription()).isEqualTo("New description");
        assertThat(environment.getUsage()).isEqualTo(EnvironmentUsage.OTHER);
    }
}
