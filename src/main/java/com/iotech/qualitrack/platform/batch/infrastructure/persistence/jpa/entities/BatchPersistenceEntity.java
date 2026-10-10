package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity representing a production Batch table.
 *
 * <p>The batch number is unique per laboratory. {@code environment_id} is nullable so batches
 * registered before environments existed keep working until they are assigned to one.</p>
 */
@Entity
@Table(name = "batches", uniqueConstraints = @UniqueConstraint(
        name = "uk_batches_laboratory_batch_number", columnNames = {"laboratory_id", "batch_number"}))
@Getter
@Setter
@NoArgsConstructor
public class BatchPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "laboratory_id", nullable = false)
    private Long labId;

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    /**
     * Converted automatically by BatchStatusPersistenceConverter.
     */
    @Column(nullable = false, length = 50)
    private BatchStatus status;

    @Column(name = "start_date", nullable = false, length = 30)
    private String startDate;

    @Column(name = "end_date", length = 30)
    private String endDate;

    @Column(length = 500)
    private String notes;

    /**
     * Container monitor of the monitored container where the batch is stored (US78).
     */
    @Column(name = "container_monitor_id")
    private Long containerMonitorId;

    @Column(name = "container_environment_id")
    private Long containerEnvironmentId;

    @Column(name = "container_assigned_by")
    private Long containerAssignedBy;

    @Column(name = "container_assigned_at")
    private java.time.Instant containerAssignedAt;
}