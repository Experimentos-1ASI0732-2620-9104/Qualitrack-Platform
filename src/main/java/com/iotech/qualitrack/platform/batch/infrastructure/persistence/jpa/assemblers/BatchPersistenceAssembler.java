package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.ContainerAssignment;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.BatchPersistenceEntity;

/**
 * Static assembler between batch domain and persistence representations.
 */
public final class BatchPersistenceAssembler {

    private BatchPersistenceAssembler() {
    }

    public static Batch toDomainFromPersistence(BatchPersistenceEntity entity) {
        if (entity == null) return null;

        return new Batch(
                entity.getId(),
                entity.getLabId(),
                entity.getEnvironmentId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getBatchNumber(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getNotes(),
                entity.getContainerMonitorId() == null ? null : new ContainerAssignment(entity.getContainerMonitorId(),
                        entity.getContainerEnvironmentId(), entity.getContainerAssignedBy(), entity.getContainerAssignedAt())
        );
    }

    public static BatchPersistenceEntity toPersistenceFromDomain(Batch batch) {
        if (batch == null) return null;

        var entity = new BatchPersistenceEntity();

        if (batch.getId() != null) {
            entity.setId(batch.getId());
        }

        entity.setLabId(batch.getLabId());
        entity.setEnvironmentId(batch.getEnvironmentId());
        entity.setProductId(batch.getProductId());
        entity.setProductName(batch.getProductName());
        entity.setBatchNumber(batch.getBatchNumber());
        entity.setQuantity(batch.getQuantity());
        entity.setUnit(batch.getUnit());
        entity.setStatus(batch.getStatus());
        entity.setStartDate(batch.getStartDate());
        entity.setEndDate(batch.getEndDate());
        entity.setNotes(batch.getNotes());
        batch.container().ifPresent(container -> {
            entity.setContainerMonitorId(container.containerMonitorId());
            entity.setContainerEnvironmentId(container.environmentId());
            entity.setContainerAssignedBy(container.assignedBy());
            entity.setContainerAssignedAt(container.assignedAt());
        });

        return entity;
    }
}