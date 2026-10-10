package com.iotech.qualitrack.platform.ra.domain.model.commands;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Command to request the generation of a production batch report.
 *
 * @param batchId The numeric identifier of the production batch. Cannot be null or less than 1.
 * @param includeDeviations Indicates whether deviation alerts should be included. Cannot be null.
 * @param format The requested output format. Cannot be null.
 * @param requestedBy The numeric identifier of the user requesting the report. Cannot be null or less than 1.
 */
public record GenerateBatchReportCommand(
        Long batchId,
        Boolean includeDeviations,
        ReportFormat format,
        Long requestedBy
) {
    /**
     * Compact constructor for GenerateBatchReportCommand.
     * Enforces Fail-Fast validation.
     */
    public GenerateBatchReportCommand {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("batchId cannot be null or less than 1");
        }
        if (includeDeviations == null) {
            throw new IllegalArgumentException("includeDeviations cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        if (requestedBy == null || requestedBy <= 0) {
            throw new IllegalArgumentException("requestedBy cannot be null or less than 1");
        }
    }
}