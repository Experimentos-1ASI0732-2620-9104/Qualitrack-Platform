package com.iotech.qualitrack.platform.ra.domain.model.commands;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Command to request the generation of a laboratory compliance report.
 *
 * @param environmentId Optional environment; null covers every environment of the laboratory.
 * @param laboratoryId The numeric identifier of the laboratory. Cannot be null or less than 1.
 * @param startDate The beginning of the reporting period. Cannot be null or blank.
 * @param endDate The end of the reporting period. Cannot be null or blank.
 * @param format The requested output format. Cannot be null.
 * @param requestedBy The numeric identifier of the user requesting the report. Cannot be null or less than 1.
 */
public record GenerateComplianceReportCommand(
        Long laboratoryId,
        Long environmentId,
        String startDate,
        String endDate,
        ReportFormat format,
        Long requestedBy
) {
    /**
     * Compact constructor for GenerateComplianceReportCommand.
     * Enforces Fail-Fast validation.
     */
    public GenerateComplianceReportCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId != null && environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be less than 1");
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