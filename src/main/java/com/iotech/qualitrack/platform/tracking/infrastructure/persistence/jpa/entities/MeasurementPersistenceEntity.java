package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
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
 * Reading of an IoT device stored in {@code telemetry_measurements}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "telemetry_measurements", indexes = {
        @Index(name = "idx_telemetry_measurements_device_time", columnList = "equipment_id, measured_at")
})
public class MeasurementPersistenceEntity extends AuditableAbstractPersistenceEntity {

    private Long laboratoryId;

    private Long environmentId;

    /**
     * IoT device that produced the reading.
     */
    @Column(nullable = false)
    private Long equipmentId;

    @Column(nullable = false, length = 120)
    private String parameterName;

    private Double value;

    @Column(length = 120)
    private String textValue;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(nullable = false, length = 50)
    private String timestamp;

    private Instant measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EnvironmentalState state;

    private Double thresholdValue;

    private Long profileVersion;
}
