package com.iotech.qualitrack.platform.inventory.domain.model.queries;

/**
 * One lot (receipt) of a raw material kept in an environment.
 */
public record GetRawMaterialBatchByIdQuery(Long laboratoryId, Long environmentId, Long rawMaterialId, Long rawMaterialBatchId) { }
