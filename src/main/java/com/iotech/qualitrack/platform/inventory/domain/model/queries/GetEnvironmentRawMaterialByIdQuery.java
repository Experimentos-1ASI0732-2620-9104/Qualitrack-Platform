package com.iotech.qualitrack.platform.inventory.domain.model.queries;

/**
 * One raw material kept in an environment with its derived stock (TS26).
 */
public record GetEnvironmentRawMaterialByIdQuery(Long laboratoryId, Long environmentId, Long rawMaterialId) { }
