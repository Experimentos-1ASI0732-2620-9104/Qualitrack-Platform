package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.commandservices.RaCommandService;
import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportsByBatchIdQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.GenerateBatchReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.AuditReportResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.GenerateBatchReportCommandFromResourceAssembler;
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
 * Traceability reports of a production batch (TS84).
 */
@RestController
@RequestMapping(value = "/api/v1/batches/{batchId}/reports", produces = APPLICATION_JSON_VALUE)
public class BatchReportController {

    private final RaCommandService raCommandService;
    private final RaQueryService raQueryService;
    private final CurrentUser currentUser;

    public BatchReportController(RaCommandService raCommandService, RaQueryService raQueryService, CurrentUser currentUser) {
        this.raCommandService = raCommandService;
        this.raQueryService = raQueryService;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a batch traceability report",
            description = "Stores the PDF or CSV report requested by the authenticated user (TS84).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report generated; Location points to the report",
                    content = @Content(schema = @Schema(implementation = AuditReportResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing format or options",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account")
    })
    public ResponseEntity<?> generateBatchReport(@PathVariable Long batchId, @RequestBody GenerateBatchReportResource resource) {
        var command = GenerateBatchReportCommandFromResourceAssembler.toCommandFromResource(batchId, resource, currentUser.userId());
        return ReportResponseAssembler.toCreatedResponse(raCommandService.handle(command));
    }

    @GetMapping
    @Operation(summary = "Get the reports of a batch")
    public ResponseEntity<List<AuditReportResource>> getAuditReportsByBatchId(@PathVariable Long batchId) {
        var resources = raQueryService.handle(new GetAuditReportsByBatchIdQuery(batchId)).stream()
                .map(AuditReportResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
