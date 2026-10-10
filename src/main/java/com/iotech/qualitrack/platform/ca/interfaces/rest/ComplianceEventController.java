package com.iotech.qualitrack.platform.ca.interfaces.rest;

import com.iotech.qualitrack.platform.ca.application.queryservices.CaQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetComplianceEventsByRelatedEntityIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventSubject;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.ComplianceEventResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.ComplianceEventResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Compliance events recorded for the equipment and batches of a laboratory.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = APPLICATION_JSON_VALUE)
public class ComplianceEventController {

    private final CaQueryService caQueryService;

    public ComplianceEventController(CaQueryService caQueryService) {
        this.caQueryService = caQueryService;
    }

    @GetMapping("/laboratories/{laboratoryId}/equipments/{equipmentId}/compliance-events")
    @Operation(summary = "Get the compliance events of an equipment")
    public ResponseEntity<List<ComplianceEventResource>> getEquipmentComplianceEvents(@PathVariable Long laboratoryId,
                                                                                      @PathVariable Long equipmentId) {
        return ResponseEntity.ok(getComplianceEventResources(equipmentId, ComplianceEventSubject.EQUIPMENT));
    }

    @GetMapping("/batches/{batchId}/compliance-events")
    @Operation(summary = "Get the compliance events of a batch")
    public ResponseEntity<List<ComplianceEventResource>> getBatchComplianceEvents(@PathVariable Long batchId) {
        return ResponseEntity.ok(getComplianceEventResources(batchId, ComplianceEventSubject.BATCH));
    }

    private List<ComplianceEventResource> getComplianceEventResources(Long relatedEntityId, ComplianceEventSubject subject) {
        return caQueryService.handle(new GetComplianceEventsByRelatedEntityIdQuery(relatedEntityId, subject)).stream()
                .map(ComplianceEventResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
