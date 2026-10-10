package com.iotech.qualitrack.platform.ra.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request of the inventory report; the authenticated user requests it.
 *
 * @param environmentId Optional environment; omitted covers every environment of the laboratory.
 * @param format PDF or CSV.
 */
@Schema(name = "GenerateInventoryReportRequest", description = "Inventory report of the laboratory or of one environment",
        example = "{\"environmentId\": 4, \"format\": \"PDF\"}")
public record GenerateInventoryReportResource(
        @Schema(description = "Only this environment", example = "4", nullable = true) Long environmentId,
        @Schema(description = "PDF or CSV", example = "PDF") ReportFormat format
) {
}
