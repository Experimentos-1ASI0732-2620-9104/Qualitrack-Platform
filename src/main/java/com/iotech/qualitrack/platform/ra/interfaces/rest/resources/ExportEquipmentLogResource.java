package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;

/**
 * Resource used to request export of historical equipment logs.
 *
 * @param startDate The beginning of the exported period.
 * @param endDate The end of the exported period.
 * @param format The requested output format.
 * The authenticated user requests the report.
 */
public record ExportEquipmentLogResource(
        String startDate,
        String endDate,
        ReportFormat format
) {
}