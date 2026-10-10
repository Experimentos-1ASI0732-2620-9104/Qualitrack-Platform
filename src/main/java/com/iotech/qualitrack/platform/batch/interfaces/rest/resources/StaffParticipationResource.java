package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Staff member who took part in a product batch.
 */
@Schema(name = "StaffParticipationResponse", description = "Staff member who took part in a product batch")
public record StaffParticipationResource(
        @Schema(description = "Participation identifier", example = "1") Long id,
        @Schema(description = "Product batch", example = "1") Long batchId,
        @Schema(description = "Staff member identifier", example = "6") Long staffId,
        @Schema(description = "Staff name when it was associated", example = "Ana Torres") String staffName,
        @Schema(description = "Staff role when it was associated", example = "Production operator") String staffRole,
        @Schema(description = "User who registered the participation", example = "3") Long registeredByUserId,
        @Schema(description = "Registration time (UTC)", example = "2026-10-02T15:04:05Z") String registeredAt
) {
}
