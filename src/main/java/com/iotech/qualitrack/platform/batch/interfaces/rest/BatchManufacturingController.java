package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.commandservices.BatchCommandService;
import com.iotech.qualitrack.platform.batch.application.commandservices.BatchParticipationCommandService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.commands.AssignBatchContainerCommand;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchContainerQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchContainerResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.batch.application.commandservices.RawMaterialUsageCommandService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchTraceabilityQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchTraceabilityQuery;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterEquipmentUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterStaffParticipationCommand;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.*;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchParticipationResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.BatchTraceabilityResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.RawMaterialUsageResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST controller for the resources that take part in the manufacturing of a product batch (TS65-TS67), the
 * container where it is stored (TS68, TS69) and its traceability (TS70).
 *
 * <p>Tenant isolation of every path identifier is enforced by the IAM tenant interceptor and the batch
 * must belong to the product of the environment in the path. Authorized laboratory staff register the
 * manufacturing records (US75-US77).</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/products/{productId}/batches/{batchId}",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class BatchManufacturingController {

    private final RawMaterialUsageCommandService rawMaterialUsageCommandService;
    private final BatchParticipationCommandService participationCommandService;
    private final BatchTraceabilityQueryService traceabilityQueryService;
    private final BatchCommandService batchCommandService;
    private final BatchQueryService batchQueryService;

    public BatchManufacturingController(RawMaterialUsageCommandService rawMaterialUsageCommandService,
                                        BatchParticipationCommandService participationCommandService,
                                        BatchTraceabilityQueryService traceabilityQueryService,
                                        BatchCommandService batchCommandService, BatchQueryService batchQueryService) {
        this.batchCommandService = batchCommandService;
        this.batchQueryService = batchQueryService;
        this.rawMaterialUsageCommandService = rawMaterialUsageCommandService;
        this.participationCommandService = participationCommandService;
        this.traceabilityQueryService = traceabilityQueryService;
    }

    @PostMapping(value = "/raw-material-usages", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a raw material usage",
            description = "Consumes the amount from a released Inventory lot and keeps which lot the batch used. "
                    + "Retrying with the same operationId returns the original usage.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw material consumed and usage recorded",
                    content = @Content(schema = @Schema(implementation = RawMaterialUsageResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid amount or unit", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch or raw material lot not found", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Closed batch, unusable lot, insufficient stock or operationId reused with other values",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerRawMaterialUsage(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                      @PathVariable Long productId, @PathVariable Long batchId,
                                                      @Valid @RequestBody RegisterRawMaterialUsageResource resource) {
        var command = new RegisterRawMaterialUsageCommand(laboratoryId, environmentId, productId, batchId,
                resource.rawMaterialBatchId(), resource.amountUsed(), resource.unit(), resource.operationId());
        return ResponseEntityAssembler.toResponseEntityFromResult(rawMaterialUsageCommandService.handle(command),
                RawMaterialUsageResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @PostMapping(value = "/equipment-usages", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register an equipment usage", description = "Associates an operational equipment of the laboratory with the batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipment associated",
                    content = @Content(schema = @Schema(implementation = EquipmentUsageResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch or equipment not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Closed batch, equipment in maintenance or out of service, or already associated",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerEquipmentUsage(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                    @PathVariable Long productId, @PathVariable Long batchId,
                                                    @Valid @RequestBody RegisterEquipmentUsageResource resource) {
        var command = new RegisterEquipmentUsageCommand(laboratoryId, environmentId, productId, batchId, resource.equipmentId());
        return ResponseEntityAssembler.toResponseEntityFromResult(participationCommandService.handle(command),
                BatchParticipationResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @PostMapping(value = "/staff-participations", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a staff participation", description = "Associates a staff member of the laboratory with the batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff member associated",
                    content = @Content(schema = @Schema(implementation = StaffParticipationResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch or staff member not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Closed batch or staff member already associated",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerStaffParticipation(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                        @PathVariable Long productId, @PathVariable Long batchId,
                                                        @Valid @RequestBody RegisterStaffParticipationResource resource) {
        var command = new RegisterStaffParticipationCommand(laboratoryId, environmentId, productId, batchId, resource.staffId());
        return ResponseEntityAssembler.toResponseEntityFromResult(participationCommandService.handle(command),
                BatchParticipationResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @GetMapping("/container-assignment")
    @Operation(summary = "Get the container of a product batch",
            description = "Monitored container and environment where the batch is stored (US79, TS69).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Container of the batch",
                    content = @Content(schema = @Schema(implementation = BatchContainerResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product, or the batch has no container",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getContainerAssignment(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                    @PathVariable Long productId, @PathVariable Long batchId) {
        return batchQueryService.handle(new GetBatchContainerQuery(laboratoryId, environmentId, productId, batchId))
                .<ResponseEntity<?>>map(container -> ResponseEntity.ok(BatchContainerResourceFromEntityAssembler.toResourceFromEntity(container)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("ContainerAssignment", batchId)));
    }

    @PutMapping(value = "/container-assignment", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Store a product batch in a container",
            description = "Stores the batch in an operational container monitor located in a product storage environment of "
                    + "the laboratory, replacing the previous container (US78, TS68).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch stored in the container",
                    content = @Content(schema = @Schema(implementation = BatchContainerResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing container monitor", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Container not registered, not in a product storage environment or not operational",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> assignContainer(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @PathVariable Long productId, @PathVariable Long batchId,
                                             @RequestBody AssignBatchContainerResource resource) {
        var command = new AssignBatchContainerCommand(laboratoryId, environmentId, productId, batchId,
                resource == null ? null : resource.containerMonitorId());
        return ResponseEntityAssembler.toResponseEntityFromResult(batchCommandService.handle(command),
                BatchContainerResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @GetMapping("/traceability")
    @Operation(summary = "Get the traceability of a batch",
            description = "Raw material lots, equipment and staff that took part in the batch, plus its release or rejection.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traceability of the batch",
                    content = @Content(schema = @Schema(implementation = BatchTraceabilityResource.class))),
            @ApiResponse(responseCode = "403", description = "Batch not available to the account"),
            @ApiResponse(responseCode = "404", description = "Batch not registered for the product", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getTraceability(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @PathVariable Long productId, @PathVariable Long batchId) {
        return traceabilityQueryService.handle(new GetBatchTraceabilityQuery(laboratoryId, environmentId, productId, batchId))
                .<ResponseEntity<?>>map(traceability -> ResponseEntity.ok(BatchTraceabilityResourceFromEntityAssembler.toResourceFromEntity(traceability)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Batch", batchId)));
    }
}
