package com.iotech.qualitrack.platform.ra.application.internal.commandservices;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.ra.application.commandservices.RaCommandService;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ComplianceReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.InventoryReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.ReportDocumentWriter;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalBatchService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalComplianceService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalEquipmentService;
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

@Service
@Transactional
public class RaCommandServiceImpl implements RaCommandService {
    private final AuditReportRepository reports;
    private final ReportDocumentRepository documents;
    private final AuditLogRepository audit;
    private final RaOperationalDataService data;
    private final ReportDocumentWriter writer;
    private final CurrentUser currentUser;
    private final IamContextFacade users;
    private final RaExternalLaboratoryService laboratories;
    private final RaExternalTrackingService tracking;
    private final RaExternalComplianceService compliance;
    private final RaExternalEquipmentService equipment;
    private final Clock reportingClock;
    private final RaExternalInventoryService inventory;
    private final RaExternalBatchService batches;

    public RaCommandServiceImpl(AuditReportRepository reports, AuditLogRepository audit,
            RaOperationalDataService data, ReportDocumentWriter writer, CurrentUser currentUser,
            IamContextFacade users, ReportDocumentRepository documents, RaExternalLaboratoryService laboratories,
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

    @Override
    public Result<AuditReport, ApplicationError> handle(GenerateBatchReportCommand command) {
        return execute("BatchReport", () -> {
            var batch = data.batch(command.batchId());
            var recordedAlerts = command.includeDeviations() ? data.batchAlerts(batch.getId())
                    : List.<com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert>of();
            var traceability = batches.findTraceability(batch.getId());
            var rows = new ArrayList<List<String>>();
            row(rows, "Batch ID", batch.getId());
            row(rows, "Batch number", batch.getBatchNumber());
            row(rows, "Product", batch.getProductName());
            row(rows, "Quantity", batch.getQuantity() + " " + batch.getUnit());
            row(rows, "Status", batch.getStatus());
            row(rows, "Started", batch.getStartDate());
            row(rows, "Finished", batch.getEndDate());
            row(rows, "Notes", batch.getNotes());
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
                row(rows, "Recorded deviations", recordedAlerts.size());
                recordedAlerts.forEach(alert -> row(rows, "Alert " + alert.getId(),
                        alert.getTimestamp() + " | " + alert.getParameterName() + " | "
                        + alert.getRecordedValue() + " " + alert.getUnit() + " | " + alert.getStatus()));
            }
            return report(ReportType.BATCH_TRACEABILITY, command.format(), batch.getLabId(),
                    batch.getId(), null, command.requestedBy(), batch.getStartDate(), batch.getEndDate(), rows,
                    command.format() == ReportFormat.PDF ? (name, at) -> writer.writeBatchPdf(
                            data.batchReport(batch, recordedAlerts, command.includeDeviations(), traceability, name, at))
                            : null);
        });
    }

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
                            data.laboratoryName(command.laboratoryId()), command.startDate(), command.endDate(), sections,
                            name, at)));
        });
    }

    /**
     * Indicators, alerts and actions of one environment in the period, from the records of Tracking and Compliance.
     */
    private ComplianceReportData.EnvironmentSection environmentSection(Long laboratoryId,
            LaboratoryContextFacade.EnvironmentReference environment, ReportingPeriod period, Map<Long, String> devices) {
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

    @Override
    public Result<AuditReport, ApplicationError> handle(ExportEquipmentLogCommand command) {
        com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment device;
        try {
            device = data.equipment(command.equipmentId());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        }
        if (!device.isLocatedIn(command.laboratoryId(), command.environmentId())) {
            return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        }
        return execute("EquipmentLog", () -> {
            validatePeriod(command.startDate(), command.endDate());
            var entries = audit.findAllByEquipmentId(device.getId()).stream()
                    .filter(entry -> inPeriod(entry.getTimestamp(), command.startDate(), command.endDate())).toList();
            return report(ReportType.EQUIPMENT_LOG, command.format(), device.getLabId(),
                    null, device.getId(), command.requestedBy(), command.startDate(), command.endDate(), new ArrayList<>(),
                    (name, at) -> writer.writeEquipment(command.format(), data.equipmentReport(device,
                            command.startDate(), command.endDate(), entries, name, at)));
        });
    }

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
                            data.laboratoryName(command.laboratoryId()), businessDate, sections, name, at)));
        });
    }

    private static String quantity(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private AuditReport report(ReportType type, ReportFormat format, Long laboratoryId, Long batchId,
            Long equipmentId, Long requestedBy, String from, String to, List<List<String>> rows) {
        return report(type, format, laboratoryId, batchId, equipmentId, requestedBy, from, to, rows, null);
    }

    private AuditReport report(ReportType type, ReportFormat format, Long laboratoryId, Long batchId,
            Long equipmentId, Long requestedBy, String from, String to, List<List<String>> rows,
            BiFunction<String, Instant, byte[]> customRenderer) {
        var actor = currentUser.userId();
        if (actor == null || !actor.equals(requestedBy)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid report requester");
        }
        var generatedAt = Instant.now();
        row(rows, "Generated at", generatedAt);
        var name = users.getUsernameByUserId(actor);
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

    private void record(AuditAction action, String type, Long id, String details) {
        audit.save(new AuditLogEntry(action, type, id, currentUser.userId(), details));
    }

    private void row(List<List<String>> rows, String label, Object value) {
        rows.add(List.of(label, value == null ? "Not recorded" : value.toString()));
    }

    private static void validatePeriod(String from, String to) {
        if (date(from).isAfter(date(to))) throw new IllegalArgumentException("Start date must not exceed end date");
    }

    private static LocalDate date(String value) {
        if (value == null || value.length() < 10) throw new IllegalArgumentException("An ISO date is required");
        return LocalDate.parse(value.substring(0, 10));
    }

    private static boolean inPeriod(String timestamp, String from, String to) {
        if (timestamp == null) return false;
        var observed = date(timestamp);
        return !observed.isBefore(date(from)) && !observed.isAfter(date(to));
    }

    private <T> Result<T, ApplicationError> execute(String entity, Supplier<T> operation) {
        try { return Result.success(operation.get()); }
        catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError(entity, exception.getMessage()));
        }
    }
}
