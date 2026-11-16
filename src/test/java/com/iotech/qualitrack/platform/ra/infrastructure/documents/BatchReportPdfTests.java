package com.iotech.qualitrack.platform.ra.infrastructure.documents;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.BatchReportData;
import com.iotech.qualitrack.platform.ra.infrastructure.documents.ReportDocumentWriterImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchReportPdfTests {
    private final ReportDocumentWriterImpl writer = new ReportDocumentWriterImpl();
    private final List<BatchReportData.Material> materials = List.of(
            new BatchReportData.Material(1L, "DEMO Paracetamol API", 31L, 500.0, "g", "2026-09-01", "1000", "500"),
            new BatchReportData.Material(2L, "DEMO Microcrystalline cellulose", null, 250.0, "g", "2026-09-01", null, null));
    private final List<BatchReportData.Deviation> deviations = List.of(
            new BatchReportData.Deviation(4L, 4L, "Temperature", 9.3, 8.0, "C", "2026-09-05T07:00:00Z", "CRITICAL", "UNRESOLVED", null),
            new BatchReportData.Deviation(5L, 5L, "Temperature", 38.4, 38.0, "C", "2026-09-05T06:00:00Z", "WARNING", "ACKNOWLEDGED", null),
            new BatchReportData.Deviation(6L, 6L, "Humidity", 68.0, 65.0, "%", "2026-09-05T05:00:00Z", "WARNING", "RESOLVED", "DEMO: Ventilation adjusted; follow-up reading pending."));

    @Test
    void emptyDeviationsProduceAShortRecordWithoutFabricatedScores() throws Exception {
        byte[] bytes = writer.writeBatchPdf(snapshot(materials, List.of(), true, "DEMO IoTech: synthetic training data, not real production or regulatory evidence."));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isLessThanOrEqualTo(2);
            assertThat(new PDFTextStripper().getText(pdf)).contains("QualiTrack", "IoTech", "Batch record", "Material traceability",
                    "DEMO Paracetamol API", "250", "2 days elapsed", "0 recorded deviations", "Billy1", "05 Sep 2026 07:16 UTC")
                    .doesNotContain("100%", "BY SEVERITY", "BATCH_TRACEABILITY", "2026-09-05T07:16");
            assertTextInsidePages(pdf);
            preview("batch-no-alerts", bytes, pdf);
        }
    }

    @Test
    void includedAlertsProduceCountsChartsAndTraceableDetails() throws Exception {
        byte[] bytes = writer.writeBatchPdf(snapshot(materials, deviations, true, "DEMO preview: synthetic alert examples for layout verification."));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("BY STATUS / COUNT", "BY SEVERITY / COUNT",
                    "Counts from 3 linked deviation record(s).", "Alert #4", "Equipment #4", "Limit: 8 C", "38.4 C",
                    "critical", "acknowledged", "Ventilation adjusted", "Equipment and staff", "Lot #31", "1000 -> 500 g",
                    "DEMO Tablet press", "DEMO Operator", "Stored in DEMO Cold room", "Released by user #7", "SHA-256");
            assertTextInsidePages(pdf);
            preview("batch-with-alerts", bytes, pdf);
        }
    }

    @Test
    void excludedAlertsAreNotLeakedIntoSummaryOrDetails() throws Exception {
        try (var pdf = Loader.loadPDF(writer.writeBatchPdf(snapshot(materials, deviations, false, "DEMO preview")))) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("Not included", "Deviation records were excluded")
                    .doesNotContain("Alert #4", "BY STATUS", "Counts from 3", "9.3", "Ventilation adjusted");
            assertTextInsidePages(pdf);
        }
    }

    @Test
    void longTablesAndNotesRemainReadableAndKeepLastRecords() throws Exception {
        var rows = IntStream.rangeClosed(1, 35).mapToObj(index ->
                new BatchReportData.Material((long) index, "DEMO Material " + index + " - " + "traceability-".repeat(12),
                        (long) index, 125.5, "milligrams", "2026-09-01", "500", "374.5")).toList();
        var notes = "DEMO: " + "A detailed observation remains part of the exported record. ".repeat(90) + "END-OF-NOTES";
        byte[] bytes = writer.writeBatchPdf(snapshot(rows, deviations, true, notes));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(2);
            assertThat(new PDFTextStripper().getText(pdf)).contains("DEMO Material 35", "END-OF-NOTES", "Page " + pdf.getNumberOfPages());
            assertTextInsidePages(pdf);
            preview("batch-multipage", bytes, pdf);
        }
    }

    private BatchReportData snapshot(List<BatchReportData.Material> materials, List<BatchReportData.Deviation> alerts,
                                     boolean included, String notes) {
        return new BatchReportData("Multilab", 5L, "DEMO-BILLY1-LOT1", "DEMO Paracetamol 500 mg",
                1000.0, "units", "RELEASED", "2026-09-01", "2026-09-03", notes, materials,
                List.of(new BatchReportData.Equipment(8L, "DEMO Tablet press", "2026-09-01T10:00:00Z")),
                List.of(new BatchReportData.Staff(9L, "DEMO Operator", "Production operator", "2026-09-01T10:05:00Z")),
                new BatchReportData.Container(12L, "DEMO Cold room", 4L, "2026-09-03T18:00:00Z"),
                new BatchReportData.Release(7L, "a1b2c3", "2026-09-03T17:00:00Z"), null, included,
                alerts, "Billy1", Instant.parse("2026-09-05T07:16:58Z"));
    }

    private void assertTextInsidePages(PDDocument pdf) throws IOException {
        new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
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
            ImageIO.write(renderer.renderImageWithDPI(page, 120), "png", directory.resolve(name + "-" + (page + 1) + ".png").toFile());
        }
    }
}
