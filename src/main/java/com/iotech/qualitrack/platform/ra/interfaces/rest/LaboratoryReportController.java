package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.commandservices.RaCommandService;
import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportsByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.GenerateComplianceReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.GenerateInventoryReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.AuditReportResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.GenerateComplianceReportCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.GenerateInventoryReportCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.ReportResponseAssembler;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Compliance reports of a laboratory (TS83) and the reports generated in it.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}", produces = APPLICATION_JSON_VALUE)
public class LaboratoryReportController {

    private final RaCommandService raCommandService;
    private final RaQueryService raQueryService;
    private final CurrentUser currentUser;

    public LaboratoryReportController(RaCommandService raCommandService, RaQueryService raQueryService,
                                      CurrentUser currentUser) {
        this.raCommandService = raCommandService;
        this.raQueryService = raQueryService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/compliance-reports", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate the environmental report of a period",
            description = "Stores the PDF or CSV report requested by the authenticated user for whole calendar days "
                    + "(America/Lima) of every environment or of environmentId (US95, TS83): average, minimum and maximum "
                    + "of the readings, time in range and deviations per device and variable, the alerts that started in "
                    + "the period and the actions of the container monitors.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report generated; Location points to the report",
                    content = @Content(schema = @Schema(implementation = AuditReportResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid period or format, or environment not in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account")
    })
    public ResponseEntity<?> generateComplianceReport(@PathVariable Long laboratoryId,
                                                      @RequestBody GenerateComplianceReportResource resource) {
        var command = GenerateComplianceReportCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource,
                currentUser.userId());
        return ReportResponseAssembler.toCreatedResponse(raCommandService.handle(command));
    }

    @PostMapping(value = "/inventory/reports", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate the inventory report",
            description = "Stores the PDF or CSV report requested by the authenticated user with the raw materials, lots and "
                    + "quantities of every environment or of environmentId (US97, TS85).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report generated; Location points to the report",
                    content = @Content(schema = @Schema(implementation = AuditReportResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing format or environment not in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account")
    })
    public ResponseEntity<?> generateInventoryReport(@PathVariable Long laboratoryId,
                                                     @RequestBody GenerateInventoryReportResource resource) {
        var command = GenerateInventoryReportCommandFromResourceAssembler.toCommandFromResource(laboratoryId, resource,
                currentUser.userId());
        return ReportResponseAssembler.toCreatedResponse(raCommandService.handle(command));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get the reports generated in the laboratory")
    public ResponseEntity<List<AuditReportResource>> getAuditReportsByLaboratoryId(@PathVariable Long laboratoryId) {
        var resources = raQueryService.handle(new GetAuditReportsByLaboratoryIdQuery(laboratoryId)).stream()
                .map(AuditReportResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
