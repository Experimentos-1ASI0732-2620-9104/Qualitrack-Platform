package com.iotech.qualitrack.platform.ra.infrastructure.documents;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.EquipmentReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.InventoryReportData;
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
    private final List<ComplianceReportData.Indicator> indicators = List.of(
            new ComplianceReportData.Indicator(12L, "DEMO Cabinet monitor", "TEMPERATURE", "C", 288, 21.4, 18.2, 33.0, 93.5, 3, 1),
            new ComplianceReportData.Indicator(11L, null, "AIR_QUALITY", "ppm", 1, 350.0, 350.0, 350.0, null, 0, 0));
    private final List<ComplianceReportData.Alert> alerts = List.of(
            new ComplianceReportData.Alert(4L, 12L, "DEMO Cabinet monitor", "CONTAINER", "TEMPERATURE", 33.0, 30.0, "C",
                    "2026-09-05T07:00:00Z", "CRITICAL", "UNRESOLVED", 2, null),
            new ComplianceReportData.Alert(6L, 12L, "DEMO Cabinet monitor", "CONTAINER", "HUMIDITY", 68.0, 65.0, "%",
                    "2026-09-05T05:00:00Z", "WARNING", "RESOLVED", 1, "DEMO: Ventilation adjusted; follow-up reading pending."));
    private final List<ComplianceReportData.Action> actions = List.of(
            new ComplianceReportData.Action(9L, 12L, "DEMO Cabinet monitor", "COOLING_ON", "TEMPERATURE", "CRITICAL", "EXECUTED",
                    "2026-09-05T07:00:05Z"));
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

    @Test void environmentalPdfShowsIndicatorsAlertsActionsAndMethod() throws Exception {
        var bytes = writer.writeCompliance(ReportFormat.PDF, compliance(indicators, alerts, actions));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(checkedText(pdf)).contains("Environmental report", "01 Sep 2026 to 05 Sep 2026",
                    "Environment WH-01 - Cold storage", "DEMO Cabinet monitor", "21.4 / 18.2 / 33 C", "93.5 %",
                    "3 (1 critical)", "Device #11", "Not enough", "Alert #4", "Limit: 30 C", "2 deviations",
                    "cooling on", "Ventilation adjusted", "NORMAL time", "not a regulatory certification", "Billy1");
            preview("environmental-report", bytes, pdf);
        }
    }

    @Test void inventoryPdfShowsStockLotsExpirationAndContainers() throws Exception {
        var bytes = writer.writeInventory(ReportFormat.PDF, inventory());
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(checkedText(pdf)).contains("Inventory report", "RAW MATERIALS", "BELOW MINIMUM", "EXPIRING",
                    "Environment WH-RM-01 - Raw materials", "PA-001", "Paracetamol API", "40 / 100 kg", "low",
                    "LOT-2026-001", "Quimica Andina", "near expiry", "Cold cabinet", "No raw materials are kept", "Billy1");
            preview("inventory-report", bytes, pdf);
        }
    }

    @Test void inventoryCsvHasOneColumnPerValue() {
        String csv = new String(writer.writeInventory(ReportFormat.CSV, inventory()), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Code\",\"Raw material\",\"Unit\",\"Minimum stock\"",
                "\"PA-001\",\"Paracetamol API\",\"kg\",\"50\",\"40\",\"100\",\"LOW\"",
                "\"PA-001\",\"7\",\"Quimica Andina\",\"LOT-2026-001\",\"100\",\"40\"",
                "\"NEAR_EXPIRY\",\"Cold cabinet\"");
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
        var compliance = writer.writeCompliance(ReportFormat.PDF, compliance(List.of(), List.of(), List.of()));
        try (var pdf = Loader.loadPDF(compliance)) {
            assertThat(checkedText(pdf)).contains("No readings in the selected period", "No alerts started",
                    "No actions of container monitors").doesNotContain("%", "Alert #");
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
        String csv = new String(writer.writeCompliance(ReportFormat.CSV, compliance(indicators, alerts, actions)), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Device\",\"Variable\",\"Unit\",\"Readings\",\"Average\"",
                "\"DEMO Cabinet monitor\",\"TEMPERATURE\",\"C\",\"288\",\"21.4\",\"18.2\",\"33.0\",\"93.5\",\"3\",\"1\"",
                "\"Alert ID\",\"Origin\",\"Device\",\"Detected at\"",
                "\"4\",\"CONTAINER\",\"DEMO Cabinet monitor\",\"2026-09-05T07:00:00Z\",\"TEMPERATURE\",\"33.0\",\"30.0\",\"C\"",
                "\"9\",\"DEMO Cabinet monitor\",\"COOLING_ON\",\"TEMPERATURE\",\"CRITICAL\",\"EXECUTED\"");
    }

    @Test void equipmentCsvPreservesDetailsAndProtectsSpreadsheetFormulaCells() {
        var maliciousCell = List.of(new EquipmentReportData.LogEntry(7L, "2026-09-01T09:00:00", "UPDATE", 38L, "=1+2,\"quoted\"\nsecond line"));
        String csv = new String(writer.writeEquipment(ReportFormat.CSV, equipment(parameters, maintenance, maliciousCell)), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Entry ID\",\"Recorded at\",\"Action\",\"Actor ID\",\"Details\"",
                "\"'=1+2,\"\"quoted\"\"\nsecond line\"", "\"Parameter\",\"Minimum\",\"Maximum\",\"Unit\"");
    }

    private ComplianceReportData compliance(List<ComplianceReportData.Indicator> indicators,
            List<ComplianceReportData.Alert> alerts, List<ComplianceReportData.Action> actions) {
        return new ComplianceReportData(1L, "DEMO Multilab", "2026-09-01", "2026-09-05", List.of(
                new ComplianceReportData.EnvironmentSection(4L, "WH-01", "Cold storage", indicators, alerts, actions)),
                "Billy1", generatedAt);
    }

    private InventoryReportData inventory() {
        var lot = new InventoryReportData.Lot(7L, "Quimica Andina", "LOT-2026-001", "100", "40", "2026-09-01",
                "2026-09-20", "RELEASED", "NEAR_EXPIRY", "Cold cabinet");
        var material = new InventoryReportData.Material(3L, "PA-001", "Paracetamol API", "kg", "50", "40", "100", "LOW", List.of(lot));
        return new InventoryReportData(1L, "DEMO Multilab", "2026-09-05", List.of(
                new InventoryReportData.EnvironmentInventory(4L, "WH-RM-01", "Raw materials", List.of(material)),
                new InventoryReportData.EnvironmentInventory(5L, "PROD-01", "Production", List.of())), "Billy1", generatedAt);
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
