package com.iotech.qualitrack.platform.inventory.domain.model.commands;

/**
 * Records the quality review of a raw material lot of a raw material kept in an environment.
 */
public record ReviewRawMaterialBatchCommand(Long laboratoryId, Long environmentId, Long materialId, Long receiptId,
                                            com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus status,
                                            String reason) { }
