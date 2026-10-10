package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchResource;

/**
 * Assembler to convert a Batch entity to a BatchResource.
 */
public class BatchResourceFromEntityAssembler {

    /**
     * Converts a Batch entity to a BatchResource.
     *
     * @param entity The {@link Batch} entity to convert.
     * @return The {@link BatchResource} resource that results from the conversion.
     */
    public static BatchResource toResourceFromEntity(Batch entity) {
        return new BatchResource(
                entity.getId(),
                entity.getLabId(),
                entity.getEnvironmentId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getBatchNumber(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getStatus().name(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getNotes(),
                entity.container().map(container -> container.containerMonitorId()).orElse(null)
        );
    }
}