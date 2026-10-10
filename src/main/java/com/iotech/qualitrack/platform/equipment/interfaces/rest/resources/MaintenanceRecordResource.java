package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Maintenance performed on an equipment.
 */
@Schema(name = "MaintenanceRecordResponse", description = "Maintenance performed on an equipment")
public record MaintenanceRecordResource(
        @Schema(description = "Maintenance record unique numeric identifier", example = "1") Long id,
        @Schema(description = "Associated equipment numeric identifier", example = "1") Long equipmentId,
        @Schema(description = "Environment where the equipment was located, null for older records", example = "2", nullable = true)
        Long environmentId,
        @Schema(description = "Date the maintenance was performed (ISO 8601)", example = "2026-05-20") String maintenanceDate,
        @Schema(description = "Name of the technician who performed the maintenance", example = "John Smith") String technicianName,
        @Schema(description = "Staff member who performed the maintenance, null for records with a typed name", example = "4",
                nullable = true) Long technicianStaffId,
        @Schema(description = "Detailed description of the intervention", example = "Annual calibration and sensor replacement")
        String description,
        @Schema(description = "Type of maintenance performed", example = "PREVENTIVE",
                allowableValues = {"PREVENTIVE", "CORRECTIVE", "CALIBRATION", "INSPECTION", "OTHER"}) String type
) {
}
