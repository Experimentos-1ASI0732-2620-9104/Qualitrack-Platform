package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditLogQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditLogEntryResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.AuditLogEntryResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Audit log of a production batch (TS80). Entries are recorded by the platform when the batch changes.
 */
@RestController
@RequestMapping(value = "/api/v1/batches/{batchId}/audit-logs", produces = APPLICATION_JSON_VALUE)
public class BatchAuditLogController {

    private final RaQueryService raQueryService;

    public BatchAuditLogController(RaQueryService raQueryService) {
        this.raQueryService = raQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the audit log of a batch", description = "Optional period: dateFrom and dateTo.")
    public ResponseEntity<List<AuditLogEntryResource>> getBatchAuditLogs(@PathVariable Long batchId,
                                                                         @RequestParam(required = false) String dateFrom,
                                                                         @RequestParam(required = false) String dateTo) {
        var resources = raQueryService.handle(new GetAuditLogQuery(null, batchId, dateFrom, dateTo)).stream()
                .map(AuditLogEntryResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
