package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.EquipmentReportData;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import com.iotech.qualitrack.platform.ra.infrastructure.documents.ReportDocumentWriterImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodReportTests {
    private final ReportDocumentWriterImpl writer = new ReportDocumentWriterImpl();
    private final Instant generatedAt = Instant.parse("2026-09-05T07:16:58Z");
    private final List<ComplianceReportData.Alert> alerts = List.of(
            new ComplianceReportData.Alert(4L, 6L, "Cold chamber", "ENVIRONMENTAL_DEVICE", "TEMPERATURE", 9.3, 8.0, "C", "2026-09-05T07:00:00Z", "CRITICAL", "UNRESOLVED", 1, null),
            new ComplianceReportData.Alert(5L, 7L, "Cold chamber", "CONTAINER_MONITOR", "TEMPERATURE", 38.4, 38.0, "C", "2026-09-05T06:00:00Z", "WARNING", "ACKNOWLEDGED", 2, null),
            new ComplianceReportData.Alert(6L, 8L, null, "CONTAINER_MONITOR", "HUMIDITY", 68.0, 65.0, "%", "2026-09-05T05:00:00Z", "WARNING", "RESOLVED", 1, "DEMO: Ventilation adjusted; follow-up reading pending."));
    private final List<EquipmentReportData.Parameter> parameters = List.of(
            new EquipmentReportData.Parameter("Temperature", 2.0, 8.0, "C"),
            new EquipmentReportData.Parameter("Humidity", 30.0, 65.0, "%"));
    private final List<EquipmentReportData.Maintenance> maintenance = List.of(
            new EquipmentReportData.Maintenance(3L, "2026-09-01", "CALIBRATION", "DEMO Technician", "DEMO: Temperature probe comparison recorded for training."),
            new EquipmentReportData.Maintenance(4L, "2026-09-03", "PREVENTIVE", "DEMO Technician", "DEMO: Ventilation filter inspected."));
    private final List<EquipmentReportData.LogEntry> entries = List.of(
            new EquipmentReportData.LogEntry(7L, "2026-09-01T09:00:00", "CREATE", 38L, "DEMO: Equipment registered."),
            new EquipmentReportData.LogEntry(8L, "2026-09-02T14:30:00", "UPDATE", 38L, "DEMO: Temperature limits updated."),
            new EquipmentReportData.LogEntry(9L, "2026-09-05T07:00:00Z", "UPDATE", null, "DEMO: Monitoring status updated by the system."));

    @Test void compliancePdfShowsCountsReferencesThresholdsAndScope() throws Exception {
        var bytes = writer.writeCompliance(ReportFormat.PDF, compliance(alerts));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(checkedText(pdf)).contains("Environmental report", "01 Sep 2026 to 05 Sep 2026",
                    "READINGS", "DEVIATIONS", "ALERTS", "acknowledged", "critical", "Alert #4",
                    "Limit: 8", "Ventilation adjusted", "not a regulatory certification", "Billy1");
            preview("compliance-report", bytes, pdf);
        }
    }

    @Test void equipmentPdfShowsCurrentLimitsMaintenanceAndDatedAuditHistory() throws Exception {
        var bytes = writer.writeEquipment(ReportFormat.PDF, equipment(parameters, maintenance, entries));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(checkedText(pdf)).contains("Equipment activity report", "DEMO Cold chamber", "SN-DEMO-4",
                    "Current configured limits", "AUDIT ACTIONS", "MAINTENANCE TYPES", "calibration", "DEMO Technician",
                    "User #38", "System", "Ventilation filter inspected", "No uptime, calibration validity");
            preview("equipment-report", bytes, pdf);
        }
    }

    @Test void emptyPeriodsDoNotInventChartsScoresOrMaintenance() throws Exception {
        var compliance = writer.writeCompliance(ReportFormat.PDF, compliance(List.of()));
        try (var pdf = Loader.loadPDF(compliance)) {
            assertThat(checkedText(pdf)).contains("No deviation records").doesNotContain("ALERTS BY", "100%");
            preview("compliance-empty", compliance, pdf);
        }
        var equipment = writer.writeEquipment(ReportFormat.PDF, equipment(List.of(), List.of(), List.of()));
        try (var pdf = Loader.loadPDF(equipment)) {
            assertThat(checkedText(pdf)).contains("No audit entries", "No maintenance records", "No parameter limits")
                    .doesNotContain("AUDIT ACTIONS", "MAINTENANCE TYPES", "100%");
            preview("equipment-empty", equipment, pdf);
        }
    }

    @Test void longEquipmentLogRepeatsHeadingsAndPreservesLastEntry() throws Exception {
        var many = IntStream.rangeClosed(1, 20).mapToObj(index -> new EquipmentReportData.LogEntry((long) index,
                "2026-09-03T11:00:00", "UPDATE", 38L, "DEMO record " + index + ": " + "Detailed observation. ".repeat(12))).toList();
        var bytes = writer.writeEquipment(ReportFormat.PDF, equipment(parameters, maintenance, many));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(2);
            assertThat(checkedText(pdf)).contains("DEMO record 20:", "Generated by Billy1", "Page " + pdf.getNumberOfPages());
            preview("equipment-multipage", bytes, pdf);
        }
    }

    @Test void complianceCsvHasIndependentDataColumnsRatherThanPackedText() {
        String csv = new String(writer.writeCompliance(ReportFormat.CSV, compliance(alerts)), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Alert ID\",\"Equipment ID\",\"Batch ID\",\"Recorded at\"",
                "\"4\",\"4\",\"6\",\"2026-09-05T07:00:00Z\",\"Temperature\",\"9.3\",\"8.0\",\"C\"");
    }

    @Test void equipmentCsvPreservesDetailsAndProtectsSpreadsheetFormulaCells() {
        var maliciousCell = List.of(new EquipmentReportData.LogEntry(7L, "2026-09-01T09:00:00", "UPDATE", 38L, "=1+2,\"quoted\"\nsecond line"));
        String csv = new String(writer.writeEquipment(ReportFormat.CSV, equipment(parameters, maintenance, maliciousCell)), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Entry ID\",\"Recorded at\",\"Action\",\"Actor ID\",\"Details\"",
                "\"'=1+2,\"\"quoted\"\"\nsecond line\"", "\"Parameter\",\"Minimum\",\"Maximum\",\"Unit\"");
    }

    private ComplianceReportData compliance(List<ComplianceReportData.Alert> alerts) {
        var indicators = List.of(new ComplianceReportData.Indicator(6L, "Cold chamber", "TEMPERATURE", "C", 120, 5.1, 2.0, 9.3, 96.5, 3, 1));
        var section = new ComplianceReportData.EnvironmentSection(1L, "ENV-1", "Cold room", indicators, alerts, List.of());
        return new ComplianceReportData(1L, "DEMO Multilab", "2026-09-01", "2026-09-05", List.of(section), "Billy1", generatedAt);
    }

    private EquipmentReportData equipment(List<EquipmentReportData.Parameter> parameters,
            List<EquipmentReportData.Maintenance> maintenance, List<EquipmentReportData.LogEntry> entries) {
        return new EquipmentReportData(4L, "DEMO Cold chamber", "DEMO Multilab", "Refrigerator", "DEMO-RX100",
                "SN-DEMO-4", "OPERATIONAL", "DEMO-IOTECH-4", "2026-09-01", "2026-09-05", parameters,
                maintenance, entries, "Billy1", generatedAt);
    }

    private String checkedText(PDDocument pdf) throws IOException {
        return new PDFTextStripper() {
            @Override protected void processTextPosition(TextPosition text) {
                assertThat(text.getXDirAdj()).isGreaterThanOrEqualTo(39);
                assertThat(text.getXDirAdj() + text.getWidthDirAdj()).isLessThanOrEqualTo(556);
                assertThat(text.getYDirAdj()).isBetween(25f, 822f);
                super.processTextPosition(text);
            }
        }.getText(pdf);
    }

    private void preview(String name, byte[] bytes, PDDocument pdf) throws IOException {
        if (!Boolean.getBoolean("qualitrack.reports.previews")) return;
        var directory = Path.of("target", "report-tests");
        Files.createDirectories(directory);
        Files.write(directory.resolve(name + ".pdf"), bytes);
        var renderer = new PDFRenderer(pdf);
        for (int page = 0; page < pdf.getNumberOfPages(); page++) {
            ImageIO.write(renderer.renderImageWithDPI(page, 105), "png", directory.resolve(name + "-" + (page + 1) + ".png").toFile());
        }
    }
}
