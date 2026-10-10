package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.commandservices.BatchCommandService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByProductQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductBatchQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.*;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.CreateBatchCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.RejectBatchCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.ReleaseBatchCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for the manufacturing batches of a product (TS63, TS64, TS71, TS72).
 *
 * <p>Tenant isolation of {@code laboratoryId}, {@code environmentId}, {@code productId} and {@code batchId}
 * is enforced by the IAM tenant interceptor; every operation also checks that the batch belongs to the
 * product of the environment in the path. Authorized laboratory staff register batches (US73); releases
 * and rejections require a quality role (US81, US82).</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/products/{productId}/batches",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class ProductBatchesController {

    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final BatchCommandService batchCommandService;
    private final BatchQueryService batchQueryService;

    public ProductBatchesController(BatchCommandService batchCommandService, BatchQueryService batchQueryService) {
        this.batchCommandService = batchCommandService;
        this.batchQueryService = batchQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a product batch", description = "Registers a pending manufacturing batch of the product.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Batch registered",
                    content = @Content(schema = @Schema(implementation = BatchResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Product not available to the account"),
            @ApiResponse(responseCode = "404", description = "Product not registered in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Batch number already used in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                           @PathVariable Long productId, @Valid @RequestBody CreateBatchResource resource) {
        var command = CreateBatchCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, productId, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(batchCommandService.handle(command),
                BatchResourceFromEntityAssembler::toResourceFromEntity, Batch::getId);
    }

    @GetMapping
    @Operation(summary = "List product batches", description = "Manufacturing batches of the product, newest start date first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batches of the product",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BatchResource.class)))),
            @ApiResponse(responseCode = "403", description = "Product not available to the account"),
            @ApiResponse(responseCode = "404", description = "Product not registered in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getBatches(@PathVariable Long laboratoryId, @PathVariable Long environmentId, @PathVariable Long productId) {
        return batchQueryService.handle(new GetBatchesByProductQuery(laboratoryId, environmentId, productId))
                .<ResponseEntity<?>>map(batches -> ResponseEntity.ok(batches.stream()
                        .map(BatchResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("PharmaceuticalProduct", productId)));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get a product batch", description = "One manufacturing batch of the product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch found",
                    content = @Content(schema = @Schema(implementation = BatchResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                      @PathVariable Long productId, @PathVariable Long batchId) {
        return batchQueryService.handle(new GetProductBatchQuery(laboratoryId, environmentId, productId, batchId))
                .<ResponseEntity<?>>map(batch -> ResponseEntity.ok(BatchResourceFromEntityAssembler.toResourceFromEntity(batch)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Batch", batchId)));
    }

    @PostMapping(value = "/{batchId}/releases", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Release a product batch", description = "Releases the batch and signs the release on behalf of the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Batch released",
                    content = @Content(schema = @Schema(implementation = BatchReleaseResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Batch already released or rejected", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> releaseBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                          @PathVariable Long productId, @PathVariable Long batchId,
                                          @Valid @RequestBody ReleaseBatchResource resource) {
        var command = ReleaseBatchCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, productId, batchId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(batchCommandService.handle(command),
                ReleaseBatchCommandFromResourceAssembler::toResourceFromRelease, HttpStatus.CREATED);
    }

    @PostMapping(value = "/{batchId}/rejections", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Reject a product batch", description = "Rejects the batch and keeps the reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Batch rejected",
                    content = @Content(schema = @Schema(implementation = BatchRejectionResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Batch already released or rejected", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> rejectBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                         @PathVariable Long productId, @PathVariable Long batchId,
                                         @Valid @RequestBody RejectBatchResource resource) {
        var command = RejectBatchCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, productId, batchId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(batchCommandService.handle(command),
                RejectBatchCommandFromResourceAssembler::toResourceFromRejection, HttpStatus.CREATED);
    }
}
