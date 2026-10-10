package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rejection of a product batch with its reason.
 */
@Schema(name = "BatchRejectionResponse", description = "Rejection of a product batch")
public record BatchRejectionResource(
        @Schema(description = "Rejection record identifier", example = "1") Long id,
        @Schema(description = "Rejected batch", example = "1") Long batchId,
        @Schema(description = "Batch traceability number", example = "PB-2026-001") String batchNumber,
        @Schema(description = "Resulting status", example = "REJECTED") String status,
        @Schema(description = "Rejection date", example = "2026-10-10") String rejectionDate,
        @Schema(description = "Reason for the rejection", example = "Failed final quality control validation") String reason
) {
}
