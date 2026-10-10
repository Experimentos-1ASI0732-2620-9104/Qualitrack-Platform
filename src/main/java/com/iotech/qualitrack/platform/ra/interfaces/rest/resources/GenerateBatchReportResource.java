package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Resource used to request generation of a production batch report.
 *
 * @param includeDeviations Indicates whether deviation alerts should be included.
 * @param format The requested output format.
 * The authenticated user requests the report.
 */
public record GenerateBatchReportResource(
        Boolean includeDeviations,
        ReportFormat format
) {
}