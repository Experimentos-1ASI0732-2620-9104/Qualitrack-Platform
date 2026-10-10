package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Registered change in the operational status of an equipment.
 */
@Schema(name = "EquipmentStatusChangeResponse", description = "Change in the operational status of an equipment")
public record EquipmentStatusChangeResource(
        @Schema(description = "Status change identifier", example = "1") Long id,
        @Schema(description = "Equipment identifier", example = "5") Long equipmentId,
        @Schema(description = "Environment where the equipment was located", example = "2") Long environmentId,
        @Schema(description = "Status before the change", example = "OPERATIONAL") String previousStatus,
        @Schema(description = "Status after the change", example = "MAINTENANCE") String newStatus,
        @Schema(description = "Reason for the change", example = "Scheduled calibration", nullable = true) String reason,
        @Schema(description = "User who registered the change", example = "3") Long changedByUserId,
        @Schema(description = "Moment of the change (ISO 8601)", example = "2026-10-03T15:30:00Z") String changedAt
) {
}
