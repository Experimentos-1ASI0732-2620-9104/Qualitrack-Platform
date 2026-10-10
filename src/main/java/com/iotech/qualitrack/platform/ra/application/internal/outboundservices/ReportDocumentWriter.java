package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import java.util.List;

public interface ReportDocumentWriter {
    byte[] write(ReportFormat format, String title, List<List<String>> rows);
    byte[] writeBatchPdf(BatchReportData data);
    byte[] writeCompliance(ReportFormat format, ComplianceReportData data);
    byte[] writeEquipment(ReportFormat format, EquipmentReportData data);
    byte[] writeInventory(ReportFormat format, InventoryReportData data);
}
