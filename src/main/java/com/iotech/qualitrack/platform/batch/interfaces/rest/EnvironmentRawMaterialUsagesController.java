package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalInventoryService;
import com.iotech.qualitrack.platform.batch.application.queryservices.RawMaterialUsageQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsagesByInventoryMaterialQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.RawMaterialUsageResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.RawMaterialUsageResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Traceability of the product batches that consumed a raw material kept in an environment (TS79, US89).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/raw-materials/{rawMaterialId}/usages",
        produces = APPLICATION_JSON_VALUE)
public class EnvironmentRawMaterialUsagesController {

    private final RawMaterialUsageQueryService queries;
    private final BatchExternalInventoryService inventory;

    public EnvironmentRawMaterialUsagesController(RawMaterialUsageQueryService queries, BatchExternalInventoryService inventory) {
        this.queries = queries;
        this.inventory = inventory;
    }

    @GetMapping
    @Operation(summary = "List the product batches that used a raw material",
            description = "Newest first. Each usage identifies the product batch, the consumed lot and the amount. "
                    + "An empty list means no product batch used the raw material.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usages of the raw material",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RawMaterialUsageResource.class)))),
            @ApiResponse(responseCode = "403", description = "Raw material not available to the account"),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getUsages(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                       @PathVariable Long rawMaterialId) {
        if (!inventory.isRawMaterialInEnvironment(laboratoryId, environmentId, rawMaterialId)) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Material", rawMaterialId));
        }
        return ResponseEntity.ok(queries.handle(new GetRawMaterialUsagesByInventoryMaterialQuery(rawMaterialId)).stream()
                .map(RawMaterialUsageResourceFromEntityAssembler::toResourceFromEntity).toList());
    }
}
