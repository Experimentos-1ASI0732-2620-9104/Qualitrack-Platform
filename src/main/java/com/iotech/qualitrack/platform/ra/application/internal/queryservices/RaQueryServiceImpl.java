package com.iotech.qualitrack.platform.ra.application.internal.queryservices;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.ra.domain.model.aggregates.KpiDashboard;
import com.iotech.qualitrack.platform.ra.domain.model.entities.AuditLogEntry;
import com.iotech.qualitrack.platform.ra.domain.model.entities.DeviationTrend;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditLogQuery;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportByIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportsByBatchIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportsByEquipmentIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportsByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetDeviationTrendsByEnvironmentQuery;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.EnvironmentalReading;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.MeasurementSummary;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalTrackingService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetKpiDashboardByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditLogRepository;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that executes Reporting and Analysis queries.
 *
 * <p>This service coordinates read-side use cases: the indicators of a period calculated on request from the
 * persisted readings (US93, US94), audit logs and generated audit reports.</p>
 */
@Service
public class RaQueryServiceImpl implements RaQueryService {
    private final com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaOperationalDataService data;

    private final AuditLogRepository auditLogRepository;
    private final AuditReportRepository auditReportRepository;
    private final com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalLaboratoryService laboratories;
    private final RaExternalTrackingService tracking;

    public RaQueryServiceImpl(
            AuditLogRepository auditLogRepository,
            AuditReportRepository auditReportRepository,
            com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaOperationalDataService data,
            com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalLaboratoryService laboratories,
            RaExternalTrackingService tracking
    ) {
        this.laboratories = laboratories;
        this.tracking = tracking;
        this.auditLogRepository = auditLogRepository;
        this.auditReportRepository = auditReportRepository;
        this.data = data;
    }

    /**
     * Handles the indicators of a laboratory: operational counts and the average, minimum and maximum of the readings
     * of each device and metric of its environments in the period (US93, TS81).
     *
     * @param query The laboratory, optional environment and period.
     * @return The indicators, or empty when the requested environment is not in the laboratory.
     */
    @Override
    public Optional<KpiDashboard> handle(GetKpiDashboardByLaboratoryIdQuery query) {
        var environments = query.environmentId() == null
                ? laboratories.findEnvironments(query.laboratoryId())
                : laboratories.findEnvironment(query.laboratoryId(), query.environmentId()).map(List::of).orElse(null);
        if (environments == null) return Optional.empty();
        var counts = data.dashboard(query.laboratoryId());
        var summaries = environments.stream()
                .flatMap(environment -> MeasurementSummary.summarize(environment.id(),
                        tracking.findReadings(query.laboratoryId(), environment.id(), query.period())).stream())
                .toList();
        return Optional.of(new KpiDashboard(query.laboratoryId(), query.period(), counts.getMetrics(), summaries,
                counts.getTimestamp()));
    }

    /**
     * Handles the deviation indicators of the variables of an environment in a period (US94, TS82).
     *
     * @param query The environment and period.
     * @return One trend per device, variable and unit with numeric readings, or empty when the environment is not in
     * the laboratory.
     */
    @Override
    public Optional<List<DeviationTrend>> handle(GetDeviationTrendsByEnvironmentQuery query) {
        if (laboratories.findEnvironment(query.laboratoryId(), query.environmentId()).isEmpty()) return Optional.empty();
        var groups = new java.util.LinkedHashMap<List<Object>, List<EnvironmentalReading>>();
        tracking.findReadings(query.laboratoryId(), query.environmentId(), query.period()).stream()
                .filter(EnvironmentalReading::isNumeric)
                .forEach(reading -> groups.computeIfAbsent(List.of(reading.deviceId(), reading.metric(),
                        java.util.Objects.toString(reading.unit(), "")), key -> new java.util.ArrayList<>()).add(reading));
        return Optional.of(groups.values().stream()
                .map(readings -> DeviationTrend.fromReadings(query.environmentId(), readings.getFirst().deviceId(),
                        readings.getFirst().metric(), readings.getFirst().unit(), readings))
                .sorted(java.util.Comparator.comparing(DeviationTrend::getEquipmentId)
                        .thenComparing(DeviationTrend::getParameterName))
                .toList());
    }

    /**
     * Handles retrieval of audit log entries using optional filters.
     *
     * @param query The query containing optional equipment, batch, and date filters.
     * @return List of audit log entries matching the provided filters.
     */
    @Override
    public Optional<List<AuditLogEntry>> handle(com.iotech.qualitrack.platform.ra.domain.model.queries.GetStaffActivityQuery query) {
        return laboratories.findStaffAccount(query.laboratoryId(), query.staffId())
                .map(account -> account.map(auditLogRepository::findAllByPerformedBy).orElse(List.of()).stream()
                        .sorted(java.util.Comparator.comparing(AuditLogEntry::getTimestamp,
                                java.util.Comparator.nullsLast(java.util.Comparator.<String>naturalOrder())).reversed()
                                .thenComparing(AuditLogEntry::getId, java.util.Comparator.reverseOrder()))
                        .toList());
    }

    @Override
    public List<AuditLogEntry> handle(GetAuditLogQuery query) {
        var hasEquipmentFilter = query.equipmentId() != null;
        var hasBatchFilter = query.batchId() != null;
        var hasDateRange = hasValidDateRange(query.dateFrom(), query.dateTo());

        if (hasEquipmentFilter && hasDateRange) {
            return auditLogRepository.findAllByEquipmentIdAndDateRange(
                    query.equipmentId(),
                    query.dateFrom(),
                    query.dateTo()
            );
        }

        if (hasBatchFilter && hasDateRange) {
            return auditLogRepository.findAllByBatchIdAndDateRange(
                    query.batchId(),
                    query.dateFrom(),
                    query.dateTo()
            );
        }

        if (hasEquipmentFilter) {
            return auditLogRepository.findAllByEquipmentId(query.equipmentId());
        }

        if (hasBatchFilter) {
            return auditLogRepository.findAllByBatchId(query.batchId());
        }

        if (hasDateRange) {
            return auditLogRepository.findAllByDateRange(query.dateFrom(), query.dateTo());
        }

        return auditLogRepository.findAll();
    }

    /**
     * Handles retrieval of an audit report by its unique identifier.
     *
     * @param query The query containing the report identifier.
     * @return The matching audit report, if found.
     */
    @Override
    public Optional<AuditReport> handle(GetAuditReportByIdQuery query) {
        return auditReportRepository.findById(query.reportId());
    }

    /**
     * Handles retrieval of generated audit reports for a laboratory.
     *
     * @param query The query containing the laboratory identifier.
     * @return List of generated audit reports associated with the laboratory.
     */
    @Override
    public List<AuditReport> handle(GetAuditReportsByLaboratoryIdQuery query) {
        return auditReportRepository.findAllByLaboratoryId(query.laboratoryId());
    }

    /**
     * Handles retrieval of generated audit reports for a production batch.
     *
     * @param query The query containing the batch identifier.
     * @return List of generated audit reports associated with the batch.
     */
    @Override
    public List<AuditReport> handle(GetAuditReportsByBatchIdQuery query) {
        return auditReportRepository.findAllByBatchId(query.batchId());
    }

    /**
     * Handles retrieval of generated audit reports for an equipment.
     *
     * @param query The query containing the equipment identifier.
     * @return List of generated audit reports associated with the equipment.
     */
    @Override
    public List<AuditReport> handle(GetAuditReportsByEquipmentIdQuery query) {
        return auditReportRepository.findAllByEquipmentId(query.equipmentId());
    }

    /**
     * Determines whether a date range filter is complete.
     *
     * @param dateFrom Lower timestamp bound.
     * @param dateTo Upper timestamp bound.
     * @return true if both date bounds are present and not blank.
     */
    private boolean hasValidDateRange(String dateFrom, String dateTo) {
        return dateFrom != null && !dateFrom.isBlank()
                && dateTo != null && !dateTo.isBlank();
    }
}
