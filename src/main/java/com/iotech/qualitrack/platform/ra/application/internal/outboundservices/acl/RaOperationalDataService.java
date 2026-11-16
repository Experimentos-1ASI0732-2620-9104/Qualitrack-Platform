package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import com.iotech.qualitrack.platform.ca.interfaces.acl.ComplianceContextFacade;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.BatchReportData;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.EquipmentReportData;
import com.iotech.qualitrack.platform.ra.domain.model.aggregates.KpiDashboard;
import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.domain.model.entities.KpiMetric;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.KpiMetricStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Translates the records that Product Batch, Equipment, Compliance and Laboratory share through their facades into
 * the data of the batch and equipment reports and the operational counts of the indicators, without manufacturing
 * observations or regulatory targets.
 */
@Service
@Transactional(readOnly = true)
public class RaOperationalDataService {
    private final RaExternalBatchService batches;
    private final RaExternalEquipmentService equipment;
    private final RaExternalComplianceService compliance;
    private final RaExternalLaboratoryService laboratories;

    public RaOperationalDataService(RaExternalBatchService batches, RaExternalEquipmentService equipment,
            RaExternalComplianceService compliance, RaExternalLaboratoryService laboratories) {
        this.batches = batches;
        this.equipment = equipment;
        this.compliance = compliance;
        this.laboratories = laboratories;
    }

    /**
     * Alerts recorded for a batch, as the deviations of its report.
     */
    public List<BatchReportData.Deviation> batchDeviations(Long batchId) {
        return compliance.findBatchAlerts(batchId).stream()
                .map(item -> new BatchReportData.Deviation(item.id(), item.deviceId(), item.parameterName(),
                        item.recordedValue(), item.thresholdValue(), item.unit(), item.recordedAt(), item.severity(),
                        item.status(), item.resolutionNotes())).toList();
    }

    /**
     * Assembles the batch report snapshot. Traceability short-circuits redundant queries:
     * materials fall back to {@code findMaterialUses} when traceability is absent,
     * while equipment/staff default to empty lists to avoid extra calls.
     */
    public BatchReportData batchReport(BatchContextFacade.BatchReference batch, List<BatchReportData.Deviation> deviations,
            boolean includeDeviations, Optional<BatchContextFacade.TraceabilityReference> traceability,
            String generatedBy, Instant generatedAt) {
        var materials = traceability.map(BatchContextFacade.TraceabilityReference::materials)
                .orElseGet(() -> batches.findMaterialUses(batch.id())).stream()
                .map(item -> new BatchReportData.Material(item.rawMaterialId(), item.name(), item.lotId(),
                        item.quantity(), item.unit(), item.usedOn(), quantity(item.stockBefore()),
                        quantity(item.stockAfter()))).toList();
        var equipment = traceability.map(trace -> trace.equipment().stream()
                .map(item -> new BatchReportData.Equipment(item.equipmentId(), item.name(), item.registeredAt())).toList())
                .orElse(List.of());
        var staff = traceability.map(trace -> trace.staff().stream()
                .map(item -> new BatchReportData.Staff(item.staffId(), item.name(), item.role(), item.registeredAt())).toList())
                .orElse(List.of());
        var container = traceability.map(BatchContextFacade.TraceabilityReference::container)
                .map(item -> new BatchReportData.Container(item.containerMonitorId(), item.name(), item.environmentId(),
                        item.assignedAt() == null ? null : item.assignedAt().toString())).orElse(null);
        var release = traceability.map(BatchContextFacade.TraceabilityReference::release)
                .map(item -> new BatchReportData.Release(item.signedByUserId(), item.signatureHash(), item.signedAt())).orElse(null);
        var rejection = traceability.map(BatchContextFacade.TraceabilityReference::rejection)
                .map(item -> new BatchReportData.Rejection(item.rejectionDate(), item.reason())).orElse(null);
        return new BatchReportData(laboratories.laboratoryName(batch.laboratoryId()), batch.id(), batch.batchNumber(),
                batch.productName(), batch.quantity(), batch.unit(), batch.status(), batch.startDate(), batch.endDate(),
                batch.notes(), materials, equipment, staff, container, release, rejection, includeDeviations,
                includeDeviations ? deviations : List.of(), generatedBy, generatedAt);
    }

    /** Formats a BigDecimal quantity without trailing zeros; null is preserved (not turned into 0). */
    private static String quantity(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    /**
     * Assembles the equipment report snapshot. Maintenance entries are filtered to the
     * [from, to] window and sorted chronologically, as are audit entries.
     */
    public EquipmentReportData equipmentReport(EquipmentContextFacade.EquipmentLogReference device, String from, String to,
            List<AuditLogEntry> recordedEntries, String generatedBy, Instant generatedAt) {
        var start = LocalDate.parse(from.substring(0, 10));
        var end = LocalDate.parse(to.substring(0, 10));
        var parameters = device.parameters().stream()
                .map(item -> new EquipmentReportData.Parameter(item.parameterName(), item.minValue(), item.maxValue(),
                        item.unit())).toList();
        var interventions = device.maintenance().stream()
                .filter(item -> item.performedOn() != null && !item.performedOn().isBefore(start)
                        && !item.performedOn().isAfter(end))
                .sorted(Comparator.comparing(EquipmentContextFacade.MaintenanceEntry::performedOn))
                .map(item -> new EquipmentReportData.Maintenance(item.id(), item.performedOn().toString(), item.type(),
                        item.technicianName(), item.description())).toList();
        var entries = recordedEntries.stream().sorted(Comparator.comparing(AuditLogEntry::getTimestamp))
                .map(item -> new EquipmentReportData.LogEntry(item.getId(), item.getTimestamp(), item.getAction().name(),
                        item.getPerformedBy(), item.getDetails())).toList();
        return new EquipmentReportData(device.id(), device.name(), laboratories.laboratoryName(device.laboratoryId()),
                device.type(), device.model(), device.serialNumber(), device.status(), device.sensorExternalId(), from, to,
                parameters, interventions, entries, generatedBy, generatedAt);
    }

    /**
     * Assembles the equipment report snapshot. Maintenance entries are filtered to the
     * [from, to] window and sorted chronologically, as are audit entries.
     */
    public KpiDashboard dashboard(Long laboratoryId) {
        var devices = equipment.equipmentIds(laboratoryId);
        var lots = batches.findBatches(laboratoryId);
        var metrics = new ArrayList<KpiMetric>();
        var recordedAt = LocalDateTime.now().toString();
        if (!devices.isEmpty()) {
            metrics.add(count("equipment-count", devices.size(), recordedAt));
            var recordedAlerts = compliance.findDeviceAlerts(devices);
            if (!recordedAlerts.isEmpty()) {
                metrics.add(count("unresolved-alert-count", recordedAlerts.stream()
                        .filter(ComplianceContextFacade.AlertReference::isOpen).count(), recordedAt));
            }
        }
        if (!lots.isEmpty()) {
            metrics.add(count("batch-count", lots.size(), recordedAt));
            metrics.add(count("released-batch-count", lots.stream()
                    .filter(BatchContextFacade.BatchReference::isReleased).count(), recordedAt));
        }
        return new KpiDashboard(null, laboratoryId, recordedAt, null, List.copyOf(metrics));
    }

    /** Builds a "count" metric with no target and UNKNOWN status. */
    private KpiMetric count(String name, long value, String at) {
        return new KpiMetric(null, name, (double) value, "count", null, KpiMetricStatus.UNKNOWN, at);
    }
}
