package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Resource used to request generation of a regulatory compliance report.
 *
 * @param environmentId Optional environment; omitted covers every environment.
 * @param startDate The beginning of the reporting period.
 * @param endDate The end of the reporting period.
 * @param format The requested output format.
 * The authenticated user requests the report.
 */
public record GenerateComplianceReportResource(
        Long environmentId,
        String startDate,
        String endDate,
        ReportFormat format
) {
}