package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByLabIdQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes production batches nested under laboratories.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/batches", produces = APPLICATION_JSON_VALUE)
public class LaboratoryBatchesController {

    private final BatchQueryService batchQueryService;

    public LaboratoryBatchesController(BatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    @GetMapping
    @Operation(summary = "Get laboratory batches", description = "Retrieves production batches by laboratory ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batches retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid laboratory ID")
    })
    public ResponseEntity<List<BatchResource>> getBatchesByLaboratoryId(
            @PathVariable
            Long laboratoryId
    ) {
        var batches = batchQueryService.handle(new GetBatchesByLabIdQuery(laboratoryId));

        var resources = batches.stream()
                .map(BatchResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }
}