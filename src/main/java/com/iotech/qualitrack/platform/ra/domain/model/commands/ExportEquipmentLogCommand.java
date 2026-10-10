package com.iotech.qualitrack.platform.ra.domain.model.commands;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Command to request the export of historical equipment logs.
 *
 * @param laboratoryId The laboratory of the equipment. Cannot be null or less than 1.
 * @param environmentId The environment where the equipment is located. Cannot be null or less than 1.
 * @param equipmentId The numeric identifier of the equipment. Cannot be null or less than 1.
 * @param startDate The beginning of the exported period. Cannot be null or blank.
 * @param endDate The end of the exported period. Cannot be null or blank.
 * @param format The requested output format. Cannot be null.
 * @param requestedBy The numeric identifier of the user requesting the export. Cannot be null or less than 1.
 */
public record ExportEquipmentLogCommand(
        Long laboratoryId,
        Long environmentId,
        Long equipmentId,
        String startDate,
        String endDate,
        ReportFormat format,
        Long requestedBy
) {
    /**
     * Compact constructor for ExportEquipmentLogCommand.
     * Enforces Fail-Fast validation.
     */
    public ExportEquipmentLogCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("equipmentId cannot be null or less than 1");
        }
        if (startDate == null || startDate.isBlank()) {
            throw new IllegalArgumentException("startDate cannot be null or blank");
        }
        if (endDate == null || endDate.isBlank()) {
            throw new IllegalArgumentException("endDate cannot be null or blank");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        if (requestedBy == null || requestedBy <= 0) {
            throw new IllegalArgumentException("requestedBy cannot be null or less than 1");
        }
    }
}