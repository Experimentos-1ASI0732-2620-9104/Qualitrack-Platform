package com.iotech.qualitrack.platform.ra.application.commandservices;

import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.ra.domain.model.commands.ExportEquipmentLogCommand;
import com.iotech.qualitrack.platform.ra.domain.model.commands.GenerateBatchReportCommand;
import com.iotech.qualitrack.platform.ra.domain.model.commands.GenerateComplianceReportCommand;
import com.iotech.qualitrack.platform.ra.domain.model.commands.GenerateInventoryReportCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service contract for Reporting and Analysis commands.
 *
 * <p>This service coordinates write-side use cases: the batch, environmental, inventory and equipment log reports
 * (US95–US98), stored as immutable audit report records.</p>
 */
public interface RaCommandService {

    /**
     * Handles the generation of a production batch report.
     *
     * @param command command containing batch report generation settings
     * @return the stored report, whose content is downloaded from /reports/{reportId}/content, or an application error
     * @see GenerateBatchReportCommand
     */
    Result<AuditReport, ApplicationError> handle(GenerateBatchReportCommand command);

    /**
     * Handles the generation of a regulatory compliance report.
     *
     * @param command command containing compliance report generation settings
     * @return the stored report or an application error
     * @see GenerateComplianceReportCommand
     */
    Result<AuditReport, ApplicationError> handle(GenerateComplianceReportCommand command);

    /**
     * Handles the export of historical equipment logs.
     *
     * @param command command containing equipment log export settings
     * @return the stored report, or not found when the equipment is not located in the environment
     * @see ExportEquipmentLogCommand
     */
    Result<AuditReport, ApplicationError> handle(ExportEquipmentLogCommand command);

    /**
     * Generates the inventory report of the laboratory or of one environment (US97, TS85).
     *
     * @param command The laboratory, optional environment and format.
     * @return The stored report, or a validation error when the environment is not in the laboratory.
     */
    Result<AuditReport, ApplicationError> handle(GenerateInventoryReportCommand command);
}