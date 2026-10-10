package com.iotech.qualitrack.platform.ra.infrastructure.documents;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.BatchReportData;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Renders the snapshot of one batch without inferring unavailable operational data. */
final class BatchReportPdfRenderer extends AbstractReportPdfRenderer {
    private BatchReportData data;

    byte[] render(BatchReportData snapshot) {
        data = snapshot;
        return renderDocument("Batch traceability report", data.batchNumber(), () -> {
            summary();
            section("01", "Batch record");
            table(List.of("Field", "Recorded value"), List.of(
                    List.of("Laboratory", value(data.laboratory())),
                    List.of("Product", value(data.product())),
                    List.of("Batch reference", value(data.batchNumber()) + " / ID " + data.batchId())), new float[]{0.25f, 0.75f});
            timeline();
            section("02", "Material traceability");
            if (data.materials().isEmpty()) {
                paragraph("No material usage records are linked to this batch.", MUTED);
            } else {
                var rows = data.materials().stream().map(material -> List.of(
                        "#" + material.id() + " " + value(material.name()),
                        material.lotId() == null ? "Not recorded" : "Lot #" + material.lotId(),
                        number(material.quantity()) + " " + value(material.unit()),
                        material.stockBefore() == null ? "Not recorded"
                                : value(material.stockBefore()) + " -> " + value(material.stockAfter()) + " " + value(material.unit()),
                        date(material.usedOn()))).toList();
                table(List.of("Material", "Lot", "Quantity", "Lot stock", "Used on"), rows,
                        new float[]{0.36f, 0.13f, 0.14f, 0.21f, 0.16f});
            }
            section("03", "Equipment and staff");
            if (data.equipment().isEmpty()) {
                paragraph("No equipment is linked to this batch.", MUTED);
            } else {
                table(List.of("Equipment", "Registered at"), data.equipment().stream().map(item -> List.of(
                        "#" + item.id() + " " + value(item.name()), timestamp(item.registeredAt()))).toList(),
                        new float[]{0.60f, 0.40f});
            }
            if (data.staff().isEmpty()) {
                paragraph("No staff members are linked to this batch.", MUTED);
            } else {
                table(List.of("Staff member", "Role", "Registered at"), data.staff().stream().map(item -> List.of(
                        "#" + item.id() + " " + value(item.name()), value(item.role()), timestamp(item.registeredAt()))).toList(),
                        new float[]{0.40f, 0.30f, 0.30f});
            }
            section("04", "Storage and quality decision");
            if (data.container() == null) {
                paragraph("The batch is not stored in a monitored container.", MUTED);
            } else {
                paragraph("Stored in " + value(data.container().name()) + " (container monitor #" + data.container().id()
                        + ") since " + timestamp(data.container().assignedAt()) + ".", INK);
            }
            if (data.release() != null) {
                paragraph("Released by user #" + data.release().signedBy() + " on " + timestamp(data.release().signedAt())
                        + ". SHA-256 signature: " + value(data.release().signatureHash()), INK);
            } else if (data.rejection() != null) {
                paragraph("Rejected on " + date(data.rejection().rejectionDate()) + ". Reason: " + value(data.rejection().reason()), INK);
            } else {
                paragraph("The batch has not been released or rejected yet.", MUTED);
            }
            if (data.deviationsIncluded() && !data.deviations().isEmpty()) ensure(132);
            section("05", "Deviation review");
            if (!data.deviationsIncluded()) {
                paragraph("Deviation records were excluded from this export. No deviation totals or charts have been calculated.", MUTED);
            } else if (data.deviations().isEmpty()) {
                paragraph("0 recorded deviations linked to this batch. This indicates no linked alert records, not a certification of product quality.", MUTED);
            } else {
                charts();
                var rows = data.deviations().stream().map(alert -> List.of(
                        "Alert #" + alert.id() + "\nEquipment #" + alert.equipmentId(),
                        timestamp(alert.recordedAt()), human(alert.parameter()),
                        number(alert.value()) + " " + value(alert.unit()) + "\nLimit: " + number(alert.threshold()) + " " + value(alert.unit()),
                        human(alert.severity()) + "\n" + human(alert.status()))).toList();
                table(List.of("Reference", "Recorded at", "Parameter", "Value / limit", "Severity / state"), rows,
                        new float[]{0.18f, 0.24f, 0.17f, 0.20f, 0.21f});
                for (var alert : data.deviations()) {
                    if (alert.resolution() != null && !alert.resolution().isBlank()) {
                        paragraph("Alert #" + alert.id() + " - Resolution: " + alert.resolution(), INK);
                    }
                }
            }
            section("06", "Notes and report scope");
            paragraph(value(data.notes()), INK);
            paragraph("Environmental conditions are not part of this report; they are consulted in Tracking and in the "
                    + "environmental report of the period.", MUTED);
            paragraph("Scope: snapshot of linked operational records. This document is not a regulatory certificate; the "
                    + "release signature is the one recorded by the platform.", MUTED);
            paragraph("Generated by " + value(data.generatedBy()) + " on " + timestamp(data.generatedAt().toString()), MUTED);
        });
    }

    private void summary() throws IOException {
        String[] labels = {"BATCH STATUS", "QUANTITY", "USAGE RECORDS", "DEVIATIONS"};
        String[] values = {human(data.status()), number(data.quantity()) + " " + value(data.unit()),
                String.valueOf(data.materials().size()), data.deviationsIncluded() ? String.valueOf(data.deviations().size()) : "Not included"};
        var wrappedValues = new ArrayList<List<String>>();
        float[] sizes = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            sizes[i] = 13;
            while (sizes[i] > 9 && width(values[i], sizes[i], bold) > WIDTH / 4 - 24) sizes[i] -= 0.5f;
            wrappedValues.add(wrap(values[i], WIDTH / 4 - 24, sizes[i], bold));
        }
        float height = Math.max(60, 39 + wrappedValues.stream().mapToInt(List::size).max().orElse(1) * 12);
        ensure(height + 9);
        rectangle(MARGIN, y, WIDTH, height, LIGHT);
        for (int i = 0; i < labels.length; i++) {
            float x = MARGIN + i * WIDTH / 4 + 12;
            text(labels[i], x, y + 10, 8, bold, MUTED);
            var lines = wrappedValues.get(i);
            for (int row = 0; row < lines.size(); row++) {
                text(lines.get(row), x, y + 27 + row * 12, sizes[i], bold,
                        i == 0 ? statusColor(data.status()) : INK);
            }
        }
        y += height + 9;
    }

    private void timeline() throws IOException {
        ensure(58);
        y += 12;
        text("STARTED", MARGIN, y, 8, bold, MUTED);
        text("FINISHED", MARGIN + WIDTH - 110, y, 8, bold, MUTED);
        text(date(data.started()), MARGIN, y + 15, 10, bold, INK);
        text(date(data.finished()), MARGIN + WIDTH - 110, y + 15, 10, bold, INK);
        rule(MARGIN + 130, y + 19, WIDTH - 270, LINE);
        rectangle(MARGIN + 127, y + 16, 6, 6, TEAL);
        rectangle(MARGIN + WIDTH - 143, y + 16, 6, 6, data.finished() == null ? LINE : TEAL);
        String duration = "Processing window";
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.parse(data.started().substring(0, 10)), LocalDate.parse(data.finished().substring(0, 10)));
            if (days >= 0) duration = days + (days == 1 ? " day" : " days") + " elapsed";
        } catch (RuntimeException unavailable) { /* Missing or invalid dates must not manufacture a duration. */ }
        text(duration, MARGIN + 150, y, 8, regular, MUTED);
        y += 39;
    }

    private void charts() throws IOException {
        ensure(108);
        long total = data.deviations().size();
        float groupWidth = (WIDTH - 24) / 2;
        text("BY STATUS / COUNT", MARGIN, y, 8, bold, MUTED);
        text("BY SEVERITY / COUNT", MARGIN + groupWidth + 24, y, 8, bold, MUTED);
        String[][] categories = {{"UNRESOLVED", "ACKNOWLEDGED", "RESOLVED"}, {"CRITICAL", "WARNING", "LOW"}};
        Color[][] colors = {{RED, AMBER, GREEN}, {RED, AMBER, TEAL}};
        for (int group = 0; group < 2; group++) {
            for (int index = 0; index < 3; index++) {
                String category = categories[group][index];
                int currentGroup = group;
                long count = data.deviations().stream().filter(alert -> category.equals(currentGroup == 0 ? alert.status() : alert.severity())).count();
                float x = MARGIN + group * (groupWidth + 24);
                float top = y + 20 + index * 23;
                float barWidth = groupWidth - 121;
                text(human(category), x, top, 8, regular, INK);
                rectangle(x + 88, top + 1, barWidth, 9, LIGHT);
                if (count > 0) rectangle(x + 88, top + 1, barWidth * count / total, 9, colors[group][index]);
                text(String.valueOf(count), x + groupWidth - 21, top, 9, bold, INK);
            }
        }
        text("Counts from " + total + " linked deviation record(s).", MARGIN, y + 89, 8, regular, MUTED);
        y += 108;
    }

}
