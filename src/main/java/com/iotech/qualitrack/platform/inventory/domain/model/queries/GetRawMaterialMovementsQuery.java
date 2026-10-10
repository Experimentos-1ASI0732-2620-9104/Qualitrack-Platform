package com.iotech.qualitrack.platform.inventory.domain.model.queries;

/**
 * Stock and review movements of a raw material kept in an environment.
 */
public record GetRawMaterialMovementsQuery(Long laboratoryId, Long environmentId, Long rawMaterialId) { }
