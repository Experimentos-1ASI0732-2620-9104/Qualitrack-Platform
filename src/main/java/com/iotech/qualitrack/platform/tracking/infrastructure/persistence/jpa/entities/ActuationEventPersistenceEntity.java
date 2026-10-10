package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationResult;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Action of a container monitor stored in {@code actuation_events}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "actuation_events", indexes = {
        @Index(name = "idx_actuation_events_device_time", columnList = "device_id, occurred_at")
})
public class ActuationEventPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private Long laboratoryId;

    @Column(nullable = false)
    private Long environmentId;

    @Column(nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActuationAction action;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MonitoredMetric triggerMetric;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EnvironmentalState triggerState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActuationResult result;

    @Column(nullable = false)
    private Instant occurredAt;

    private Long profileVersion;
}
