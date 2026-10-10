package com.iotech.qualitrack.platform.ra.application.queryservices;

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
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetKpiDashboardByLaboratoryIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for Reporting and Analysis read queries.
 *
 * <p>Coordinates read-side use cases for KPI dashboards, deviation trends,
 * audit reports, and audit log entries.</p>
 */
public interface RaQueryService {

    /**
     * Handles retrieval of the latest KPI dashboard for a laboratory.
     *
     * @param query Query containing the laboratory identifier.
     * @return The latest KPI dashboard snapshot, if found.
     * @see GetKpiDashboardByLaboratoryIdQuery
     */
    Optional<KpiDashboard> handle(GetKpiDashboardByLaboratoryIdQuery query);

    /**
     * Handles the deviation indicators of the variables of an environment (US94, TS82).
     *
     * @param query The environment and period.
     * @return One trend per device and variable with readings, or empty when the environment is not in the laboratory.
     * @see GetDeviationTrendsByEnvironmentQuery
     */
    Optional<List<DeviationTrend>> handle(GetDeviationTrendsByEnvironmentQuery query);

    /**
     * Handles retrieval of audit log entries using optional filters.
     *
     * @param query Query containing optional equipment, batch, and date filters.
     * @return List of audit log entries matching the provided filters.
     * @see GetAuditLogQuery
     */
    List<AuditLogEntry> handle(GetAuditLogQuery query);

    /**
     * Handles retrieval of the operations registered by a staff member, newest first.
     *
     * @return the audit entries performed with the staff member account, or empty when the staff member is not
     * registered in the laboratory
     */
    Optional<List<AuditLogEntry>> handle(com.iotech.qualitrack.platform.ra.domain.model.queries.GetStaffActivityQuery query);

    /**
     * Handles retrieval of an audit report by its unique identifier.
     *
     * @param query Query containing the report identifier.
     * @return The matching audit report, if found.
     * @see GetAuditReportByIdQuery
     */
    Optional<AuditReport> handle(GetAuditReportByIdQuery query);

    /**
     * Handles retrieval of audit reports associated with a laboratory.
     *
     * @param query Query containing the laboratory identifier.
     * @return List of audit reports associated with the laboratory.
     * @see GetAuditReportsByLaboratoryIdQuery
     */
    List<AuditReport> handle(GetAuditReportsByLaboratoryIdQuery query);

    /**
     * Handles retrieval of audit reports associated with a production batch.
     *
     * @param query Query containing the batch identifier.
     * @return List of audit reports associated with the batch.
     * @see GetAuditReportsByBatchIdQuery
     */
    List<AuditReport> handle(GetAuditReportsByBatchIdQuery query);

    /**
     * Handles retrieval of audit reports associated with an equipment.
     *
     * @param query Query containing the equipment identifier.
     * @return List of audit reports associated with the equipment.
     * @see GetAuditReportsByEquipmentIdQuery
     */
    List<AuditReport> handle(GetAuditReportsByEquipmentIdQuery query);
}