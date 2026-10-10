package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Command to register an equipment used in a product batch (US76, TS66).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment of the product
 * @param productId the product identifier
 * @param batchId the product batch
 * @param equipmentId the equipment of the laboratory that was used
 */
public record RegisterEquipmentUsageCommand(Long laboratoryId, Long environmentId, Long productId, Long batchId, Long equipmentId) {
    public RegisterEquipmentUsageCommand {
        if (batchId == null || batchId <= 0) throw new IllegalArgumentException("batchId cannot be null or less than 1");
        if (equipmentId == null || equipmentId <= 0) throw new IllegalArgumentException("equipmentId cannot be null or less than 1");
    }
}
