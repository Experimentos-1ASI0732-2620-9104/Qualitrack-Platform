package com.iotech.qualitrack.platform.inventory.domain.model.commands;

/**
 * Stores a lot of a raw material kept in an environment in a monitored container of that environment (US43, TS29).
 */
public record AssignRawMaterialBatchContainerCommand(Long laboratoryId, Long environmentId, Long materialId, Long receiptId,
                                                     Long containerMonitorId) { }
