package com.iotech.qualitrack.platform.ra.application.internal.commandservices;

import com.iotech.qualitrack.platform.ra.application.commandservices.RaCommandService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.BatchReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.InventoryReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ReportDocumentWriter;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalBatchService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalComplianceService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalEquipmentService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalIamService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalInventoryService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalLaboratoryService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalTrackingService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaOperationalDataService;
import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.ra.domain.model.commands.*;
import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.domain.model.entities.DeviationTrend;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.*;
import com.iotech.qualitrack.platform.ra.domain.repositories.*;
import com.iotech.qualitrack.platform.shared.application.result.*;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.BiFunction;

/**
 * Default implementation of the {@link RaCommandService} application port.
 *
 * <p>Central orchestrator for generating audit reports of the RA module. Its responsibilities:
 * <ul>
 *   <li>Fetching and validating input (batch, environment, equipment) through ACL services.</li>
 *   <li>Assembling the payload each report needs (rows or a custom renderer callback).</li>
 *   <li>Persisting the metadata ({@link AuditReport}) and the binary document
 *       ({@link ReportDocument}) in a single transactional unit.</li>
 *   <li>Recording the generation as an audit entry so report creation itself is auditable.</li>
 * </ul>
 * All methods run inside a Spring-managed transaction; either all side effects
 * (metadata, binary, audit) commit, or none do.
 */
@Service
@Transactional
public class RaCommandServiceImpl implements RaCommandService {

    // Persistence ports.
    private final AuditReportRepository reports;
    private final ReportDocumentRepository documents;
    private final AuditLogRepository audit;

    // Outbound application services and cross-context ACL gateways.
    private final RaOperationalDataService data;
    private final ReportDocumentWriter writer;
    private final CurrentUser currentUser;
    private final RaExternalIamService users;
    private final RaExternalLaboratoryService laboratories;
    private final RaExternalTrackingService tracking;
    private final RaExternalComplianceService compliance;
    private final RaExternalEquipmentService equipment;

    // Injected clock so business dates are deterministic and testable.
    private final Clock reportingClock;

    private final RaExternalInventoryService inventory;
    private final RaExternalBatchService batches;

    public RaCommandServiceImpl(AuditReportRepository reports, AuditLogRepository audit,
            RaOperationalDataService data, ReportDocumentWriter writer, CurrentUser currentUser,
            RaExternalIamService users, ReportDocumentRepository documents, RaExternalLaboratoryService laboratories,
            RaExternalTrackingService tracking, RaExternalComplianceService compliance,
            RaExternalEquipmentService equipment, Clock reportingClock, RaExternalInventoryService inventory,
            RaExternalBatchService batches) {
        this.batches = batches;
        this.inventory = inventory;
        this.laboratories = laboratories;
        this.tracking = tracking;
        this.compliance = compliance;
        this.equipment = equipment;
        this.reportingClock = reportingClock;
        this.reports = reports;
        this.documents = documents;
        this.audit = audit;
        this.data = data;
        this.writer = writer;
        this.currentUser = currentUser;
        this.users = users;
    }

    /**
     * Generates the batch traceability report for a single batch (TS84).
     *
     * <p>Flow:
     * <ol>
     *   <li>Fetches the batch; missing batches are surfaced as a validation error.</li>
     *   <li>Optionally loads the batch deviations, honouring {@code includeDeviations}.</li>
     *   <li>Loads the traceability snapshot (materials, equipment, staff, container,
     *       release/rejection) — any absent section is simply omitted, never fabricated.</li>
     *   <li>Builds the CSV-style row payload; PDF rendering is deferred to {@link ReportDocumentWriter}
     *       through the {@code customRenderer} callback so heavy layout runs only when needed.</li>
     * </ol>
     * The report metadata and its binary content are persisted atomically by the shared
     * {@code report(...)} helper.
     */
    @Override
    public Result<AuditReport, ApplicationError> handle(GenerateBatchReportCommand command) {
        return execute("BatchReport", () -> {
            var batch = batches.findBatch(command.batchId())
                    .orElseThrow(() -> new IllegalArgumentException("Batch does not exist"));
            var deviations = command.includeDeviations() ? data.batchDeviations(batch.id())
                    : List.<BatchReportData.Deviation>of();
            var traceability = batches.findTraceability(batch.id());
            var rows = new ArrayList<List<String>>();
            row(rows, "Batch ID", batch.id());
            row(rows, "Batch number", batch.batchNumber());
            row(rows, "Product", batch.productName());
            row(rows, "Quantity", batch.quantity() + " " + batch.unit());
            row(rows, "Status", batch.status());
            row(rows, "Started", batch.startDate());
            row(rows, "Finished", batch.endDate());
            row(rows, "Notes", batch.notes());
            traceability.ifPresent(trace -> {
                row(rows, "Raw materials used", trace.materials().size());
                trace.materials().forEach(item -> row(rows, "Material " + item.name(), item.quantity() + " " + item.unit()
                        + " | lot " + (item.lotId() == null ? "not recorded" : "#" + item.lotId()) + " | " + item.usedOn()));
                row(rows, "Equipment", trace.equipment().size());
                trace.equipment().forEach(item -> row(rows, "Equipment #" + item.equipmentId(), item.name()));
                row(rows, "Staff", trace.staff().size());
                trace.staff().forEach(item -> row(rows, "Staff #" + item.staffId(), item.name() + " | " + item.role()));
                row(rows, "Container", trace.container() == null ? null
                        : trace.container().name() + " | since " + trace.container().assignedAt());
                row(rows, "Release", trace.release() == null ? null
                        : "User #" + trace.release().signedByUserId() + " | " + trace.release().signedAt()
                        + " | SHA-256 " + trace.release().signatureHash());
                row(rows, "Rejection", trace.rejection() == null ? null
                        : trace.rejection().rejectionDate() + " | " + trace.rejection().reason());
            });
            if (command.includeDeviations()) {
                row(rows, "Recorded deviations", deviations.size());
                deviations.forEach(deviation -> row(rows, "Alert " + deviation.id(),
                        deviation.recordedAt() + " | " + deviation.parameter() + " | "
                        + deviation.value() + " " + deviation.unit() + " | " + deviation.status()));
            }
            return report(ReportType.BATCH_TRACEABILITY, command.format(), batch.laboratoryId(),
                    batch.id(), null, command.requestedBy(), batch.startDate(), batch.endDate(), rows,
                    command.format() == ReportFormat.PDF ? (name, at) -> writer.writeBatchPdf(
                            data.batchReport(batch, deviations, command.includeDeviations(), traceability, name, at))
                            : null);
        });
    }

    /**
     * Generates the environmental (compliance) report for a period (US95, TS83).
     *
     * <p>Validates the period and resolves the target environments: either all environments
     * of the laboratory, or a single one when {@code environmentId} is provided. The environment
     * is verified to belong to the laboratory; otherwise it is treated as a validation error.
     * Each environment contributes one {@link ComplianceReportData.EnvironmentSection} assembled
     * by {@link #environmentSection}.
     *
     * <p>Includes a custom PDF renderer for the {@link ReportFormat#PDF} case; CSV goes through
     * the generic writer.
     */
    @Override
    public Result<AuditReport, ApplicationError> handle(GenerateComplianceReportCommand command) {
        return execute("ComplianceReport", () -> {
            validatePeriod(command.startDate(), command.endDate());
            var period = ReportingPeriod.ofDays(date(command.startDate()), date(command.endDate()), reportingClock.getZone());
            var environments = command.environmentId() == null
                    ? laboratories.findEnvironments(command.laboratoryId())
                    : laboratories.findEnvironment(command.laboratoryId(), command.environmentId()).map(List::of)
                    .orElseThrow(() -> new IllegalArgumentException("The environment does not belong to the laboratory"));
            var devices = equipment.deviceNames(command.laboratoryId());
            var sections = environments.stream()
                    .map(environment -> environmentSection(command.laboratoryId(), environment, period, devices))
                    .toList();
            return report(ReportType.COMPLIANCE_PERIOD, command.format(), command.laboratoryId(),
                    null, null, command.requestedBy(), command.startDate(), command.endDate(), new ArrayList<>(),
                    (name, at) -> writer.writeCompliance(command.format(), new ComplianceReportData(command.laboratoryId(),
                            laboratories.laboratoryName(command.laboratoryId()), command.startDate(), command.endDate(), sections,
                            name, at)));
        });
    }

    /**
     * Indicators, alerts and actions of one environment in the period, from the records
     * of Tracking and Compliance.
     *
     * <p>Indicators are produced by {@link MeasurementSummary} and enriched with the
     * {@link DeviationTrend} metrics (time in range, deviation count, critical deviation count),
     * computed from the raw readings. Alerts and actions are projected into their DTO shapes.
     * All data is read-only; no aggregation is invented when source data is absent.
     */
    private ComplianceReportData.EnvironmentSection environmentSection(Long laboratoryId,
            ReportEnvironment environment, ReportingPeriod period, Map<Long, String> devices) {
        var readings = tracking.findReadings(laboratoryId, environment.id(), period);
        var trends = new HashMap<List<Object>, DeviationTrend>();
        readings.stream().filter(EnvironmentalReading::isNumeric)
                .collect(Collectors.groupingBy(reading -> List.<Object>of(reading.deviceId(), reading.metric(),
                        Objects.toString(reading.unit(), ""))))
                .forEach((key, group) -> trends.put(key, DeviationTrend.fromReadings(environment.id(),
                        group.getFirst().deviceId(), group.getFirst().metric(), group.getFirst().unit(), group)));
        var indicators = MeasurementSummary.summarize(environment.id(), readings).stream().map(summary -> {
            var trend = trends.get(List.<Object>of(summary.deviceId(), summary.metric(), Objects.toString(summary.unit(), "")));
            return new ComplianceReportData.Indicator(summary.deviceId(), devices.get(summary.deviceId()),
                    summary.metric(), summary.unit(), summary.readings(), summary.average(), summary.minimum(),
                    summary.maximum(), trend.getTimeInRangePercent(), trend.getDeviationCount(),
                    trend.getCriticalDeviationCount());
        }).toList();
        var alerts = compliance.findEnvironmentAlerts(laboratoryId, environment.id(), period).stream()
                .map(alert -> new ComplianceReportData.Alert(alert.id(), alert.deviceId(), devices.get(alert.deviceId()),
                        alert.origin(), alert.parameterName(), alert.recordedValue(), alert.thresholdValue(), alert.unit(),
                        alert.detectedAt().toString(), alert.severity(), alert.status(), alert.deviationCount(),
                        alert.resolutionNotes()))
                .toList();
        var actions = tracking.findActuations(laboratoryId, environment.id(), period).stream()
                .map(action -> new ComplianceReportData.Action(action.id(), action.deviceId(), devices.get(action.deviceId()),
                        action.action(), action.triggerMetric(), action.triggerState(), action.result(),
                        action.occurredAt().toString()))
                .toList();
        return new ComplianceReportData.EnvironmentSection(environment.id(), environment.code(), environment.name(),
                indicators, alerts, actions);
    }

    /**
     * Generates the equipment activity report for a period (TS86).
     *
     * <p>Before executing the transaction-bound rendering, verifies that the equipment is
     * actually located in the given environment/laboratory (otherwise returns 404). Once located,
     * validates the period, filters the equipment audit entries to the period, and delegates
     * rendering to {@link ReportDocumentWriter#writeEquipment}. Equipment identity and
     * environmental location are captured by the report metadata.
     */
    @Override
    public Result<AuditReport, ApplicationError> handle(ExportEquipmentLogCommand command) {
        var located = equipment.findEquipmentLog(command.laboratoryId(), command.environmentId(), command.equipmentId());
        if (located.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        }
        var device = located.get();
        return execute("EquipmentLog", () -> {
            validatePeriod(command.startDate(), command.endDate());
            var entries = audit.findAllByEquipmentId(device.id()).stream()
                    .filter(entry -> inPeriod(entry.getTimestamp(), command.startDate(), command.endDate())).toList();
            return report(ReportType.EQUIPMENT_LOG, command.format(), device.laboratoryId(),
                    null, device.id(), command.requestedBy(), command.startDate(), command.endDate(), new ArrayList<>(),
                    (name, at) -> writer.writeEquipment(command.format(), data.equipmentReport(device,
                            command.startDate(), command.endDate(), entries, name, at)));
        });
    }

    /**
     * Generates the laboratory inventory report (US97, TS85).
     *
     * <p>Resolves the target environments (all or a single one), then builds one
     * {@link InventoryReportData.EnvironmentInventory} per environment with its
     * materials and lots. The business date is derived from {@link #reportingClock},
     * so "expiring" and "lowStock" are consistent with the operational cut in
     * America/Lima.
     */
    @Override
    public Result<AuditReport, ApplicationError> handle(GenerateInventoryReportCommand command) {
        return execute("InventoryReport", () -> {
            var environments = command.environmentId() == null
                    ? laboratories.findEnvironments(command.laboratoryId())
                    : laboratories.findEnvironment(command.laboratoryId(), command.environmentId()).map(List::of)
                    .orElseThrow(() -> new IllegalArgumentException("The environment does not belong to the laboratory"));
            var devices = equipment.deviceNames(command.laboratoryId());
            var sections = environments.stream().map(environment -> new InventoryReportData.EnvironmentInventory(
                    environment.id(), environment.code(), environment.name(),
                    inventory.findInventory(command.laboratoryId(), environment.id()).stream()
                            .map(material -> new InventoryReportData.Material(material.id(), material.code(), material.name(),
                                    material.unit(), quantity(material.minimumStock()), quantity(material.usableStock()),
                                    quantity(material.physicalStock()), material.stockStatus(), material.lots().stream()
                                    .map(lot -> new InventoryReportData.Lot(lot.id(), lot.supplier(), lot.batchNumber(),
                                            quantity(lot.initialAmount()), quantity(lot.availableAmount()),
                                            String.valueOf(lot.receivedOn()), String.valueOf(lot.expiresOn()), lot.status(),
                                            lot.expirationStatus(), lot.containerMonitorId() == null ? null
                                            : devices.getOrDefault(lot.containerMonitorId(), "Container #" + lot.containerMonitorId())))
                                    .toList()))
                            .toList()))
                    .toList();
            var businessDate = LocalDate.now(reportingClock).toString();
            return report(ReportType.INVENTORY, command.format(), command.laboratoryId(), null, null,
                    command.requestedBy(), businessDate, businessDate, new ArrayList<>(),
                    (name, at) -> writer.writeInventory(command.format(), new InventoryReportData(command.laboratoryId(),
                            laboratories.laboratoryName(command.laboratoryId()), businessDate, sections, name, at)));
        });
    }

    /**
     * Formats a {@link BigDecimal} quantity as a plain string without trailing zeros.
     * Returns {@code null} when the value is absent, matching "Not recorded" semantics
     * in the renderer.
     */
    private static String quantity(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    /**
     * Convenience overload for reports that do not need a custom renderer.
     * Delegates to the full version with {@code customRenderer = null}.
     */
    private AuditReport report(ReportType type, ReportFormat format, Long laboratoryId, Long batchId,
            Long equipmentId, Long requestedBy, String from, String to, List<List<String>> rows) {
        return report(type, format, laboratoryId, batchId, equipmentId, requestedBy, from, to, rows, null);
    }

    /**
     * Centralized persistence routine for every report type:
     * <ol>
     *   <li>Verifies the current user matches {@code requestedBy}, preventing a forged
     *       command from generating reports under someone else's identity.</li>
     *   <li>Stamps the generation time and user name into the row payload.</li>
     *   <li>Produces the binary content: uses {@code customRenderer} when provided
     *       (PDF renderers), otherwise falls back to the generic writer.</li>
     *   <li>Persists the metadata ({@link AuditReport}) and binary ({@link ReportDocument})
     *       records, then logs a {@code GENERATE} audit entry scoped to the affected
     *       aggregate (batch, equipment, or laboratory).</li>
     * </ol>
     * Any exception aborts the transaction and rolls back all three writes.
     */
    private AuditReport report(ReportType type, ReportFormat format, Long laboratoryId, Long batchId,
            Long equipmentId, Long requestedBy, String from, String to, List<List<String>> rows,
            BiFunction<String, Instant, byte[]> customRenderer) {
        var actor = currentUser.userId();
        if (actor == null || !actor.equals(requestedBy)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid report requester");
        }
        var generatedAt = Instant.now();
        row(rows, "Generated at", generatedAt);
        var name = users.username(actor);
        row(rows, "Generated by", name);
        var bytes = customRenderer == null ? writer.write(format, "QualiTrack - " + type, rows)
                : customRenderer.apply(name, generatedAt);
        var report = reports.save(new AuditReport(laboratoryId, batchId, equipmentId, actor, name, type, from, to,
                "report-" + UUID.randomUUID() + "." + format.name().toLowerCase(java.util.Locale.ROOT), bytes));
        documents.save(new ReportDocument(report.getId(), format, bytes));
        record(AuditAction.GENERATE, equipmentId != null ? "EQUIPMENT" : batchId != null ? "BATCH" : "LABORATORY",
                equipmentId != null ? equipmentId : batchId != null ? batchId : laboratoryId,
                "Report generated from persisted operational records: " + type);
        return report;
    }

    /**
     * Persists a single audit log entry. Used by {@link #report} to make report generation
     * itself auditable. Kept private to enforce that all audit writes go through this service.
     */
    private void record(AuditAction action, String type, Long id, String details) {
        audit.save(new AuditLogEntry(action, type, id, currentUser.userId(), details));
    }

    /**
     * Appends a {@code label/value} row to the payload, normalizing {@code null} to
     * "Not recorded" (same semantics used by the PDF/CSV renderers).
     */
    private void row(List<List<String>> rows, String label, Object value) {
        rows.add(List.of(label, value == null ? "Not recorded" : value.toString()));
    }


    /**
     * Validates that the period start does not exceed the end.
     * Dates are normalized to the first 10 characters (ISO {@code YYYY-MM-DD}) so
     * callers may pass full timestamps without breaking the comparison.
     */
    private static void validatePeriod(String from, String to) {
        if (date(from).isAfter(date(to))) throw new IllegalArgumentException("Start date must not exceed end date");
    }

    /**
     * Parses the first 10 characters of an ISO-8601 string as a {@link LocalDate}.
     * Throws {@link IllegalArgumentException} when the value is missing or too short.
     */
    private static LocalDate date(String value) {
        if (value == null || value.length() < 10) throw new IllegalArgumentException("An ISO date is required");
        return LocalDate.parse(value.substring(0, 10));
    }

    /**
     * Tolerant "is timestamp within [from, to]" check used by equipment log filtering.
     * Returns {@code false} for null timestamps; compares only the date portion to keep
     * behaviour consistent with {@link #validatePeriod}.
     */
    private static boolean inPeriod(String timestamp, String from, String to) {
        if (timestamp == null) return false;
        var observed = date(timestamp);
        return !observed.isBefore(date(from)) && !observed.isAfter(date(to));
    }

    /**
     * Wraps an operation that may throw {@link IllegalArgumentException} and converts
     * it into a  with a validation error,
     * keeping controller code free of try/catch boilerplate.
     * Any other exception is propagated so unexpected failures surface loudly.
     */
    private <T> Result<T, ApplicationError> execute(String entity, Supplier<T> operation) {
        try { return Result.success(operation.get()); }
        catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError(entity, exception.getMessage()));
        }
    }
}
