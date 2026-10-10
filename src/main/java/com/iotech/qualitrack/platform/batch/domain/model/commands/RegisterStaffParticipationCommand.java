package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Command to register a staff member who took part in a product batch (US77, TS67).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment of the product
 * @param productId the product identifier
 * @param batchId the product batch
 * @param staffId the staff member of the laboratory who took part
 */
public record RegisterStaffParticipationCommand(Long laboratoryId, Long environmentId, Long productId, Long batchId, Long staffId) {
    public RegisterStaffParticipationCommand {
        if (batchId == null || batchId <= 0) throw new IllegalArgumentException("batchId cannot be null or less than 1");
        if (staffId == null || staffId <= 0) throw new IllegalArgumentException("staffId cannot be null or less than 1");
    }
}
