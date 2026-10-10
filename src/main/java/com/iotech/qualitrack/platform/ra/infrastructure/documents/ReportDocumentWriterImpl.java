package com.iotech.qualitrack.platform.ra.infrastructure.documents;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ReportDocumentWriter;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.BatchReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.EquipmentReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.InventoryReportData;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportDocumentWriterImpl implements ReportDocumentWriter {
    @Override
    public byte[] writeBatchPdf(BatchReportData data) {
        return new BatchReportPdfRenderer().render(data);
    }

    @Override
    public byte[] writeCompliance(ReportFormat format, ComplianceReportData data) {
        if (format == ReportFormat.PDF) return new PeriodReportPdfRenderer().render(data);
        var rows = new ArrayList<List<String>>();
        rows.add(List.of("Laboratory ID", value(data.laboratoryId()), "Laboratory", value(data.laboratory())));
        rows.add(List.of("From", data.from(), "To", data.to()));
        rows.add(List.of("Environments", Integer.toString(data.environments().size()), "Readings", Integer.toString(data.readings()),
                "Deviations", Integer.toString(data.deviations()), "Alerts", Integer.toString(data.alerts()),
                "Actions", Integer.toString(data.actions())));
        for (var environment : data.environments()) {
            rows.add(List.of("Environment", value(environment.code()), value(environment.name())));
            rows.add(List.of("Device", "Variable", "Unit", "Readings", "Average", "Minimum", "Maximum", "Time in range %",
                    "Deviations", "Critical deviations"));
            environment.indicators().forEach(i -> rows.add(List.of(device(i.device(), i.deviceId()), value(i.metric()),
                    value(i.unit()), Integer.toString(i.readings()), value(i.average()), value(i.minimum()), value(i.maximum()),
                    value(i.timeInRangePercent()), Integer.toString(i.deviations()), Integer.toString(i.criticalDeviations()))));
            if (environment.indicators().isEmpty()) rows.add(List.of("Observations", "No readings in the selected period."));
            rows.add(List.of("Alert ID", "Origin", "Device", "Detected at", "Variable", "Value", "Limit", "Unit", "Severity",
                    "Current status", "Deviations", "Resolution"));
            environment.alerts().forEach(a -> rows.add(List.of(value(a.id()), value(a.origin()), device(a.device(), a.deviceId()),
                    value(a.detectedAt()), value(a.parameter()), value(a.value()), value(a.threshold()), value(a.unit()),
                    value(a.severity()), value(a.status()), value(a.deviationCount()), value(a.resolution()))));
            if (environment.alerts().isEmpty()) rows.add(List.of("Observations", "No alerts started in the selected period."));
            rows.add(List.of("Action ID", "Device", "Action", "Trigger variable", "Trigger condition", "Result", "Executed at"));
            environment.actions().forEach(a -> rows.add(List.of(value(a.id()), device(a.device(), a.deviceId()), value(a.action()),
                    value(a.triggerMetric()), value(a.triggerState()), value(a.result()), value(a.occurredAt()))));
            if (environment.actions().isEmpty()) rows.add(List.of("Observations", "No actions in the selected period."));
        }
        if (data.environments().isEmpty()) rows.add(List.of("Observations", "The laboratory has no environments."));
        rows.add(List.of("Scope", "Whole calendar days (America/Lima), inclusive. Time in range: each evaluated reading keeps "
                + "its condition until the next one; NORMAL time over the time between the first and last evaluated reading. "
                + "Deviations: readings worse than the previous one. Alert status is current at export. Not a regulatory certification."));
        csvProvenance(rows, data.generatedBy(), data.generatedAt().toString());
        return write(format, "Environmental report", rows);
    }

    private static String device(String name, Long id) { return name == null ? "Device #" + id : name; }

    @Override
    public byte[] writeEquipment(ReportFormat format, EquipmentReportData data) {
        if (format == ReportFormat.PDF) return new PeriodReportPdfRenderer().render(data);
        var rows = new ArrayList<List<String>>();
        rows.add(List.of("Equipment ID", value(data.equipmentId()), "Name", value(data.name())));
        rows.add(List.of("Laboratory", value(data.laboratory()), "Type", value(data.type())));
        rows.add(List.of("Model", value(data.model()), "Serial number", value(data.serialNumber())));
        rows.add(List.of("Current status", value(data.status()), "Sensor ID", value(data.sensorId())));
        rows.add(List.of("From", data.from(), "To", data.to()));
        rows.add(List.of("Current configured limits"));
        rows.add(List.of("Parameter", "Minimum", "Maximum", "Unit"));
        data.parameters().forEach(p -> rows.add(List.of(value(p.name()), value(p.minimum()), value(p.maximum()), value(p.unit()))));
        rows.add(List.of("Maintenance records", Integer.toString(data.maintenance().size())));
        rows.add(List.of("Maintenance ID", "Date", "Type", "Technician", "Description"));
        data.maintenance().forEach(m -> rows.add(List.of(value(m.id()), value(m.date()), value(m.type()), value(m.technician()), value(m.description()))));
        if (data.maintenance().isEmpty()) rows.add(List.of("Observations", "No maintenance records in the selected period."));
        rows.add(List.of("Audit entries", Integer.toString(data.entries().size())));
        rows.add(List.of("Entry ID", "Recorded at", "Action", "Actor ID", "Details"));
        data.entries().forEach(e -> rows.add(List.of(value(e.id()), value(e.recordedAt()), value(e.action()),
                e.actorId() == null ? "System" : value(e.actorId()), value(e.details()))));
        if (data.entries().isEmpty()) rows.add(List.of("Observations", "No audit entries in the selected period."));
        rows.add(List.of("Scope", "Activity and maintenance use the selected calendar dates. Status and limits are current. No uptime or calibration validity is inferred."));
        csvProvenance(rows, data.generatedBy(), data.generatedAt().toString());
        return write(format, "Equipment log", rows);
    }

    @Override
    public byte[] writeInventory(ReportFormat format, InventoryReportData data) {
        if (format == ReportFormat.PDF) return new InventoryReportPdfRenderer().render(data);
        var rows = new ArrayList<List<String>>();
        rows.add(List.of("Laboratory ID", value(data.laboratoryId()), "Laboratory", value(data.laboratory())));
        rows.add(List.of("Business date", value(data.businessDate()), "Raw materials", Integer.toString(data.materials()),
                "Lots", Integer.toString(data.lots()), "Below minimum", Long.toString(data.lowStock())));
        for (var environment : data.environments()) {
            rows.add(List.of("Environment", value(environment.code()), value(environment.name())));
            rows.add(List.of("Code", "Raw material", "Unit", "Minimum stock", "Usable stock", "Physical stock", "Stock status"));
            environment.materials().forEach(m -> rows.add(List.of(value(m.code()), value(m.name()), value(m.unit()),
                    value(m.minimumStock()), value(m.usableStock()), value(m.physicalStock()), value(m.stockStatus()))));
            if (environment.materials().isEmpty()) rows.add(List.of("Observations", "No raw materials in this environment."));
            rows.add(List.of("Raw material", "Lot ID", "Supplier", "Lot number", "Initial", "Available",
                    "Received on", "Expires on", "Status", "Expiration", "Container"));
            environment.materials().forEach(m -> m.lots().forEach(l -> rows.add(List.of(value(m.code()), value(l.id()),
                    value(l.supplier()), value(l.batchNumber()), value(l.initialAmount()),
                    value(l.availableAmount()), value(l.receivedOn()), value(l.expiresOn()), value(l.status()),
                    value(l.expirationStatus()), value(l.container())))));
        }
        if (data.environments().isEmpty()) rows.add(List.of("Observations", "The laboratory has no environments."));
        rows.add(List.of("Scope", "Stock and lots recorded at export. Usable stock counts released, unexpired lots. "
                + "Expiration is evaluated on the business date (America/Lima)."));
        csvProvenance(rows, data.generatedBy(), data.generatedAt().toString());
        return write(format, "Inventory report", rows);
    }

    private static String value(Object value) { return value == null ? "Not recorded" : value.toString(); }

    private void csvProvenance(List<List<String>> rows, String user, String at) {
        rows.add(List.of("Generated by", value(user), "Generated at", at));
        rows.add(List.of("Data notice", "Records marked DEMO are synthetic training data, not production evidence."));
    }

    @Override
    public byte[] write(ReportFormat format, String title, List<List<String>> rows) {
        if (format == ReportFormat.CSV) {
            return rows.stream().map(row -> row.stream().map(this::csvField)
                    .collect(Collectors.joining(","))).collect(Collectors.joining("\r\n"))
                    .getBytes(StandardCharsets.UTF_8);
        }
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var lines = new ArrayList<String>();
            lines.add(title);
            lines.add("");
            for (var row : rows) lines.addAll(wrap(String.join(" | ", row), font));
            int pageNumber = 0;
            for (int start = 0; start < lines.size(); start += 46) {
                var page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, 10);
                    stream.setLeading(16);
                    stream.newLineAtOffset(48, page.getMediaBox().getHeight() - 48);
                    for (var line : lines.subList(start, Math.min(start + 46, lines.size()))) {
                        stream.showText(line);
                        stream.newLine();
                    }
                    stream.endText();
                    stream.beginText();
                    stream.setFont(font, 9);
                    stream.newLineAtOffset(48, 28);
                    stream.showText("QualiTrack | " + (++pageNumber));
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Report document could not be generated", exception);
        }
    }

    private String csvField(String value) {
        // Quoting preserves commas/newlines; prefix formula-like text for spreadsheet safety.
        var safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+@-\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private List<String> wrap(String text, PDType1Font font) throws IOException {
        var lines = new ArrayList<String>();
        var line = new StringBuilder();
        for (int point : text.codePoints().toArray()) {
            var character = new String(Character.toChars(point));
            if (Character.isISOControl(point)) character = " ";
            try { font.encode(character); }
            catch (IllegalArgumentException unsupported) { character = "[U+" + Integer.toHexString(point) + "]"; }
            if (font.getStringWidth(line + character) / 1000 * 10 > PDRectangle.A4.getWidth() - 96) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
        }
        lines.add(line.toString());
        return lines;
    }
}
