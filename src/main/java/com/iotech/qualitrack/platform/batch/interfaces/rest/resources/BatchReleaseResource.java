package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Release of a product batch with the signature of the user who confirmed it.
 */
@Schema(name = "BatchReleaseResponse", description = "Release of a product batch")
public record BatchReleaseResource(
        @Schema(description = "Released batch", example = "1") Long batchId,
        @Schema(description = "Batch traceability number", example = "PB-2026-001") String batchNumber,
        @Schema(description = "Resulting status", example = "RELEASED") String status,
        @Schema(description = "Release date", example = "2026-10-10") String releaseDate,
        @Schema(description = "Final quality remarks", example = "All quality controls passed") String notes,
        @Schema(description = "User who released the batch", example = "4") Long releasedByUserId,
        @Schema(description = "SHA-256 signature of the release", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08") String signatureHash,
        @Schema(description = "Moment of the signature (UTC)", example = "2026-10-10T15:04:05Z") String signedAt
) {
}
