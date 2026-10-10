package com.iotech.qualitrack.platform.ra.infrastructure.documents;

import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.EquipmentReportData;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Period-specific report contents using the same layout as batch reports. */
final class PeriodReportPdfRenderer extends AbstractReportPdfRenderer {
    byte[] render(ComplianceReportData data) {
        return renderDocument("Environmental report", data.laboratory() + " / Laboratory #" + data.laboratoryId(), () -> {
            metrics(List.of(new Metric("READINGS", Integer.toString(data.readings()), TEAL),
                    new Metric("DEVIATIONS", Integer.toString(data.deviations()), AMBER),
                    new Metric("ALERTS", Integer.toString(data.alerts()), RED),
                    new Metric("ACTIONS", Integer.toString(data.actions()), INK)));
            section("01", "Review period and method");
            paragraph(date(data.from()) + " to " + date(data.to()) + " (whole calendar days, America/Lima). Environments: "
                    + data.environments().size() + ".", INK);
            paragraph("Average, minimum and maximum use the numeric readings persisted for each device and variable. Time in "
                    + "range: each reading evaluated against the profile keeps its condition until the next one; it is the "
                    + "NORMAL time over the time between the first and last evaluated reading. Deviations are readings worse "
                    + "than the previous one; critical deviations entered CRITICAL.", MUTED);
            var number = 2;
            for (var environment : data.environments()) {
                section(String.format("%02d", number++), "Environment " + value(environment.code()) + " - " + value(environment.name()));
                if (environment.indicators().isEmpty()) {
                    paragraph("No readings in the selected period. No indicator can be inferred without readings.", MUTED);
                } else {
                    table(List.of("Device / variable", "Readings", "Average / min / max", "Time in range", "Deviations"),
                            environment.indicators().stream().map(i -> List.of(
                                    device(i.device(), i.deviceId()) + "\n" + human(i.metric()),
                                    Integer.toString(i.readings()),
                                    number(i.average()) + " / " + number(i.minimum()) + " / " + number(i.maximum()) + " " + value(i.unit()),
                                    i.timeInRangePercent() == null ? "Not enough readings" : number(i.timeInRangePercent()) + " %",
                                    i.deviations() + " (" + i.criticalDeviations() + " critical)")).toList(),
                            new float[]{.27f, .13f, .26f, .17f, .17f});
                }
                if (environment.alerts().isEmpty()) {
                    paragraph("No alerts started in the selected period.", MUTED);
                } else {
                    table(List.of("Alert / origin", "Detected at", "Observation / limit", "Severity / state"),
                            environment.alerts().stream().map(a -> List.of(
                                    "Alert #" + a.id() + "\n" + human(a.origin()) + ": " + device(a.device(), a.deviceId()),
                                    timestamp(a.detectedAt()),
                                    human(a.parameter()) + "\n" + number(a.value()) + " " + value(a.unit())
                                            + "\nLimit: " + number(a.threshold()) + " " + value(a.unit()),
                                    human(a.severity()) + "\n" + human(a.status()) + "\n" + value(a.deviationCount()) + (Integer.valueOf(1).equals(a.deviationCount()) ? " deviation" : " deviations")))
                                    .toList(),
                            new float[]{.27f, .22f, .27f, .24f});
                    for (var alert : environment.alerts()) {
                        if (alert.resolution() != null && !alert.resolution().isBlank()) {
                            paragraph("Alert #" + alert.id() + " - Resolution: " + alert.resolution(), INK);
                        }
                    }
                }
                if (environment.actions().isEmpty()) {
                    paragraph("No actions of container monitors in the selected period.", MUTED);
                } else {
                    table(List.of("Device", "Action", "Trigger", "Result / executed at"),
                            environment.actions().stream().map(a -> List.of(device(a.device(), a.deviceId()), human(a.action()),
                                    human(a.triggerMetric()) + "\n" + human(a.triggerState()),
                                    human(a.result()) + "\n" + timestamp(a.occurredAt()))).toList(),
                            new float[]{.27f, .23f, .22f, .28f});
                }
            }
            if (data.environments().isEmpty()) paragraph("The laboratory has no environments.", MUTED);
            section(String.format("%02d", number), "Scope and authorship");
            paragraph("Recorded readings, alerts and actions; this report is not a regulatory certification. Alert states "
                    + "reflect the time of export.", MUTED);
            provenance(data.generatedBy(), data.generatedAt());
        });
    }

    private static String device(String name, Long id) { return name == null ? "Device #" + id : name; }

    byte[] render(EquipmentReportData data) {
        return renderDocument("Equipment activity report", data.name() + " / Equipment #" + data.equipmentId(), () -> {
            metrics(List.of(new Metric("AUDIT ENTRIES", Integer.toString(data.entries().size()), TEAL),
                    new Metric("MAINTENANCE", Integer.toString(data.maintenance().size()), INK),
                    new Metric("PARAMETERS", Integer.toString(data.parameters().size()), INK),
                    new Metric("ACTORS", Long.toString(data.entries().stream().map(EquipmentReportData.LogEntry::actorId).distinct().count()), INK)));
            section("01", "Equipment and period");
            table(List.of("Field", "Recorded value"), List.of(
                    List.of("Laboratory", value(data.laboratory())),
                    List.of("Type / model", value(data.type()) + " / " + value(data.model())),
                    List.of("Serial / sensor", value(data.serialNumber()) + " / " + value(data.sensorId())),
                    List.of("Current status", human(data.status())),
                    List.of("Activity period", date(data.from()) + " to " + date(data.to()) + " (inclusive)")), new float[]{.27f, .73f});
            section("02", "Current configured limits");
            paragraph("Configuration at export time, not a reconstruction of past settings or a measurement result.", MUTED);
            if (data.parameters().isEmpty()) paragraph("No parameter limits have been recorded for this equipment.", MUTED);
            else table(List.of("Parameter", "Minimum", "Maximum", "Unit"), data.parameters().stream().map(p ->
                    List.of(value(p.name()), number(p.minimum()), number(p.maximum()), value(p.unit()))).toList(),
                    new float[]{.40f, .20f, .20f, .20f});
            section("03", "Activity in the selected period");
            if (data.entries().isEmpty()) paragraph("No audit entries in the selected period.", MUTED);
            else countChart("AUDIT ACTIONS / COUNT", counts(data.entries(), EquipmentReportData.LogEntry::action), TEAL);
            if (!data.maintenance().isEmpty()) {
                countChart("MAINTENANCE TYPES / COUNT", counts(data.maintenance(), EquipmentReportData.Maintenance::type), AMBER);
            }
            section("04", "Maintenance register");
            if (data.maintenance().isEmpty()) paragraph("No maintenance records in the selected period.", MUTED);
            else table(List.of("Reference / date", "Type", "Technician", "Recorded intervention"), data.maintenance().stream().map(m ->
                    List.of("#" + m.id() + "\n" + date(m.date()), human(m.type()), value(m.technician()), value(m.description()))).toList(),
                    new float[]{.21f, .20f, .22f, .37f});
            section("05", "Audit trail");
            if (data.entries().isEmpty()) paragraph("No recorded actions to display.", MUTED);
            else table(List.of("ID / recorded at", "Action", "Actor", "Details"), data.entries().stream().map(e ->
                    List.of("#" + e.id() + "\n" + timestamp(e.recordedAt()), human(e.action()),
                            e.actorId() == null ? "System" : "User #" + e.actorId(), value(e.details()))).toList(),
                    new float[]{.26f, .18f, .16f, .40f});
            section("06", "Scope and authorship");
            paragraph("Activity and maintenance use the selected calendar dates. Equipment status and limits are current. Actor counts include System when recorded. Exporting this report creates a new audit entry after this snapshot.", MUTED);
            paragraph("No uptime, calibration validity or regulatory compliance score is inferred from these records. Telemetry measurements are not part of this log export.", MUTED);
            provenance(data.generatedBy(), data.generatedAt());
        });
    }

    private void provenance(String user, Instant generatedAt) throws IOException {
        paragraph("Generated by " + value(user) + " on " + timestamp(generatedAt.toString()), MUTED);
        paragraph("Timestamps without a zone retain their recorded local time. Records marked DEMO are synthetic training data, not production evidence.", MUTED);
    }

    private static <T> Map<String, Long> counts(List<T> records, Function<T, String> category) {
        return records.stream().collect(Collectors.groupingBy(category, TreeMap::new, Collectors.counting()));
    }
}
