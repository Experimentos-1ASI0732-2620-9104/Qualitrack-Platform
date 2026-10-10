package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Command to reject a product batch with a reason (US82, TS72).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 * @param batchId the batch to reject
 * @param rejectionDate the rejection date in ISO 8601 format (yyyy-MM-dd)
 * @param reason the justification (maximum 500 characters)
 */
public record RejectBatchCommand(
        Long laboratoryId,
        Long environmentId,
        Long productId,
        Long batchId,
        String rejectionDate,
        String reason
) {
    public RejectBatchCommand {
        if (batchId == null || batchId <= 0) throw new IllegalArgumentException("batchId cannot be null or less than 1");
        rejectionDate = CreateBatchCommand.isoDate(rejectionDate, "rejectionDate");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason cannot be null or blank");
        reason = reason.trim();
        if (reason.length() > 500) throw new IllegalArgumentException("reason cannot exceed 500 characters");
    }
}
