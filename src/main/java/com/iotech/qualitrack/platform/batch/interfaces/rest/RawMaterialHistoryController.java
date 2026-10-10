package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.queryservices.RawMaterialUsageQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialHistoryQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.RawMaterialUsageResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.RawMaterialUsageResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/raw-materials/{legacyRawMaterialId}/usages", produces = "application/json")
public class RawMaterialHistoryController {
    private final RawMaterialUsageQueryService queries;

    public RawMaterialHistoryController(RawMaterialUsageQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    @PreAuthorize("@tenantAccess.allows('legacyRawMaterialId', #legacyRawMaterialId)")
    @Operation(summary = "Get material consumption history", description = "Newest first. Each record identifies its production batch. Stock before/after are null for legacy records that did not deduct inventory.")
    public List<RawMaterialUsageResource> history(@PathVariable Long laboratoryId, @PathVariable Long legacyRawMaterialId) {
        return queries.handle(new GetRawMaterialHistoryQuery(legacyRawMaterialId)).stream()
                .map(RawMaterialUsageResourceFromEntityAssembler::toResourceFromEntity).toList();
    }
}
