package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import java.util.List;

/**
 * Outbound port for serializing audit reports into their final binary representation
 * (PDF or CSV).
 *
 * <p>Boundary of the RA module to the rendering infrastructure: application services
 * depend only on this interface, hiding PDF Box, CSV formatting, pagination and
 * text-escaping details.
 *
 * <p>{@link #write} is the generic fallback; the other methods are specialized
 * renderers for report types whose layout cannot be expressed as a flat row list
 * (summary blocks, timelines, charts, multi-column tables).
 *
 * <p>All methods must be pure: identical input must yield byte-identical output.
 */
public interface ReportDocumentWriter {

    /** Builds a "count" metric with no target and UNKNOWN status. */
    byte[] write(ReportFormat format, String title, List<List<String>> rows);

    /** Specialized branded PDF renderer for batch traceability reports. */
    byte[] writeBatchPdf(BatchReportData data);

    /** Environmental (compliance) report; period renderer for PDF, flat CSV otherwise. */
    byte[] writeCompliance(ReportFormat format, ComplianceReportData data);

    /** Equipment activity report; period renderer for PDF, flat CSV otherwise. */
    byte[] writeEquipment(ReportFormat format, EquipmentReportData data);

    /** Inventory report; dedicated renderer for PDF, flat CSV otherwise. */
    byte[] writeInventory(ReportFormat format, InventoryReportData data);
}
