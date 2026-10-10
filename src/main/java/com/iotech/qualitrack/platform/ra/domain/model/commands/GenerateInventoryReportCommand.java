package com.iotech.qualitrack.platform.ra.domain.model.commands;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Command to generate the inventory report of a laboratory or of one of its environments (US97, TS85).
 *
 * @param laboratoryId The laboratory.
 * @param environmentId Optional environment; null covers every environment of the laboratory.
 * @param format PDF or CSV.
 * @param requestedBy The authenticated user.
 */
public record GenerateInventoryReportCommand(Long laboratoryId, Long environmentId, ReportFormat format, Long requestedBy) {
    public GenerateInventoryReportCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId != null && environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be less than 1");
        }
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        if (requestedBy == null || requestedBy <= 0) {
            throw new IllegalArgumentException("requestedBy cannot be null or less than 1");
        }
    }
}
