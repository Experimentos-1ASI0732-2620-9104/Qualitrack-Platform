package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.commandservices.RaCommandService;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.ExportEquipmentLogResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.ExportEquipmentLogCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.ReportResponseAssembler;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Log reports of an equipment located in an environment of the laboratory (TS86).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/equipments/{equipmentId}",
        produces = APPLICATION_JSON_VALUE)
public class EquipmentReportController {

    private final RaCommandService raCommandService;
    private final CurrentUser currentUser;

    public EquipmentReportController(RaCommandService raCommandService, CurrentUser currentUser) {
        this.raCommandService = raCommandService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/log-reports", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a log report of an equipment",
            description = "Stores the PDF or CSV report of the audit log of the equipment in the period, requested by "
                    + "the authenticated user (TS86).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report generated; Location points to the report",
                    content = @Content(schema = @Schema(implementation = AuditReportResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid period or format",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Equipment or environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "The equipment is not located in the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> exportEquipmentLogs(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                 @PathVariable Long equipmentId,
                                                 @RequestBody ExportEquipmentLogResource resource) {
        var command = ExportEquipmentLogCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId,
                equipmentId, resource, currentUser.userId());
        return ReportResponseAssembler.toCreatedResponse(raCommandService.handle(command));
    }
}
