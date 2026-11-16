package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByLabIdQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that lists the production batches of a laboratory, whatever their product and environment.
 *
 * <p>Tenant isolation of {@code laboratoryId} is enforced by the IAM tenant interceptor: only the staff of the
 * laboratory, auditors included, can read its batches. Batches are registered, released and rejected through
 * {@link ProductBatchesController}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/batches", produces = APPLICATION_JSON_VALUE)
public class LaboratoryBatchesController {

    private final BatchQueryService batchQueryService;

    public LaboratoryBatchesController(BatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    /**
     * Lists the production batches of the laboratory.
     *
     * @param laboratoryId the laboratory of the signed-in account
     * @return the batches of every product of the laboratory; an empty list when it has none
     */
    @GetMapping
    @Operation(summary = "List laboratory batches",
            description = "Manufacturing batches of every product of the laboratory, in any environment. "
                    + "An empty list means the laboratory has not registered batches yet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batches of the laboratory",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BatchResource.class)))),
            @ApiResponse(responseCode = "400", description = "Laboratory id is not a number",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account")
    })
    public ResponseEntity<List<BatchResource>> getBatchesByLaboratoryId(
            @Parameter(description = "Laboratory of the signed-in account", example = "1")
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