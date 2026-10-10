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
 * Audit log of an equipment of the laboratory. Entries are recorded by the platform when the equipment changes.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/equipments/{equipmentId}/audit-logs",
        produces = APPLICATION_JSON_VALUE)
public class EquipmentAuditLogController {

    private final RaQueryService raQueryService;

    public EquipmentAuditLogController(RaQueryService raQueryService) {
        this.raQueryService = raQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the audit log of an equipment", description = "Optional period: dateFrom and dateTo.")
    public ResponseEntity<List<AuditLogEntryResource>> getEquipmentAuditLogs(@PathVariable Long laboratoryId,
                                                                             @PathVariable Long equipmentId,
                                                                             @RequestParam(required = false) String dateFrom,
                                                                             @RequestParam(required = false) String dateTo) {
        var resources = raQueryService.handle(new GetAuditLogQuery(equipmentId, null, dateFrom, dateTo)).stream()
                .map(AuditLogEntryResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
