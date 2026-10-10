package com.iotech.qualitrack.platform.inventory.domain.model.commands;

/**
 * Registers a supplier receipt (raw material lot) of a raw material kept in an environment.
 */
public record ReceiveRawMaterialBatchCommand(Long laboratoryId, Long environmentId, Long materialId, String supplier,
                                             String batchNumber, String unit, java.math.BigDecimal amount,
                                             java.time.LocalDate receivedOn, java.time.LocalDate expiresOn) { }
