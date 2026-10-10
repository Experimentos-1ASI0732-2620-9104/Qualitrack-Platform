package com.iotech.qualitrack.platform.inventory.domain.model.queries;

/**
 * Monitored container where a lot of a raw material kept in an environment is stored (US44, TS30).
 */
public record GetRawMaterialBatchContainerQuery(Long laboratoryId, Long environmentId, Long rawMaterialId, Long rawMaterialBatchId) { }
