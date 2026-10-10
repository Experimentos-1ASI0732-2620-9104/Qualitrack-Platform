package com.iotech.qualitrack.platform.inventory.domain.model.queries;

/**
 * Lots (receipts) of a raw material kept in an environment (TS24).
 */
public record GetRawMaterialBatchesQuery(Long laboratoryId, Long environmentId, Long rawMaterialId) { }
