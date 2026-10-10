package com.iotech.qualitrack.platform.inventory.interfaces.rest;

import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryCommandService;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryImportService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ImportLegacyRawMaterialCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.*;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ExpirationStatus;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.StockStatus;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.*;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.transform.*;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for the raw material inventory kept in an environment (TS21-TS30).
 *
 * <p>Tenant isolation of {@code laboratoryId}, {@code environmentId}, {@code rawMaterialId} and
 * {@code rawMaterialBatchId} is enforced by the IAM tenant interceptor. Catalog changes, lot reviews and
 * legacy imports require a quality role; lot reception is open to authorized laboratory staff.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentInventoryController {

    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final InventoryCommandService commands;
    private final InventoryQueryService queries;
    private final InventoryImportService imports;
    private final Clock clock;
    private final NearExpiryPeriod nearExpiryPeriod;

    public EnvironmentInventoryController(InventoryCommandService commands, InventoryQueryService queries,
                                          InventoryImportService imports, Clock inventoryClock,
                                          NearExpiryPeriod inventoryNearExpiryPeriod) {
        this.commands = commands;
        this.queries = queries;
        this.imports = imports;
        this.clock = inventoryClock;
        this.nearExpiryPeriod = inventoryNearExpiryPeriod;
    }

    @PostMapping(value = "/raw-materials", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Register a raw material", description = "Creates the catalog entry of a raw material kept in the environment. Stock comes from its lots.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw material registered",
                    content = @Content(schema = @Schema(implementation = RawMaterialResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data or unsupported unit", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Material code already exists in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerRawMaterial(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                 @Valid @RequestBody SaveRawMaterialResource resource) {
        var command = SaveRawMaterialCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, null, resource);
        var result = commands.handle(command).map(material -> summary(laboratoryId, environmentId, material));
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(result,
                RawMaterialResourceFromEntityAssembler::toResourceFromEntity, MaterialStockSummary::id);
    }

    @GetMapping("/raw-materials")
    @Operation(summary = "List raw materials", description = "Raw materials kept in the environment with their derived stock. Use stockStatus=LOW to list materials below their minimum stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raw materials of the environment",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RawMaterialResource.class)))),
            @ApiResponse(responseCode = "400", description = "Unsupported stockStatus value", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public List<RawMaterialResource> getRawMaterials(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
            @RequestParam(required = false) @Parameter(description = "Stock classification filter", example = "LOW") StockStatus stockStatus) {
        return queries.handle(new GetEnvironmentRawMaterialsQuery(laboratoryId, environmentId, stockStatus)).stream()
                .map(RawMaterialResourceFromEntityAssembler::toResourceFromEntity).toList();
    }

    @GetMapping("/raw-materials/{rawMaterialId}")
    @Operation(summary = "Get a raw material")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raw material found", content = @Content(schema = @Schema(implementation = RawMaterialResource.class))),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getRawMaterial(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                            @PathVariable Long rawMaterialId) {
        return queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, rawMaterialId))
                .<ResponseEntity<?>>map(material -> ResponseEntity.ok(RawMaterialResourceFromEntityAssembler.toResourceFromEntity(material)))
                .orElseGet(() -> notFound("Material", rawMaterialId));
    }

    @PutMapping(value = "/raw-materials/{rawMaterialId}", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Update a raw material", description = "Replaces the catalog data of the raw material. The unit cannot change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raw material updated", content = @Content(schema = @Schema(implementation = RawMaterialResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data or unit change", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Material code already exists in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> updateRawMaterial(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                               @PathVariable Long rawMaterialId, @Valid @RequestBody SaveRawMaterialResource resource) {
        var command = SaveRawMaterialCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, rawMaterialId, resource);
        var result = commands.handle(command).map(material -> summary(laboratoryId, environmentId, material));
        return ResponseEntityAssembler.toResponseEntityFromResult(result, RawMaterialResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @GetMapping("/raw-materials/{rawMaterialId}/stock")
    @Operation(summary = "Get the usable stock of a raw material",
            description = "Usable stock is calculated from RELEASED, non-expired lots with available amount; zero when no lot is usable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock calculated", content = @Content(schema = @Schema(implementation = RawMaterialStockResource.class))),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getStock(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                      @PathVariable Long rawMaterialId) {
        return queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, rawMaterialId))
                .<ResponseEntity<?>>map(material -> ResponseEntity.ok(RawMaterialResourceFromEntityAssembler.toStockResourceFromEntity(material)))
                .orElseGet(() -> notFound("Material", rawMaterialId));
    }

    @PostMapping(value = "/raw-materials/{rawMaterialId}/batches", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a received raw material lot", description = "Registers a supplier lot in QUARANTINED status; it must be reviewed before use.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lot registered", content = @Content(schema = @Schema(implementation = ReceiptResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid lot data, dates or unit", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The supplier lot is already registered", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> receiveBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                          @PathVariable Long rawMaterialId, @Valid @RequestBody ReceiveRawMaterialBatchResource resource) {
        var command = ReceiveRawMaterialBatchCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, rawMaterialId, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(commands.handle(command),
                this::toBatchResource, RawMaterialBatch::getId);
    }

    @GetMapping("/raw-materials/{rawMaterialId}/batches")
    @Operation(summary = "List the lots of a raw material",
            description = "Lots ordered by expiration date with status and expiration classification. Use usable=true to list only lots available for consumption.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lots of the raw material",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReceiptResource.class)))),
            @ApiResponse(responseCode = "404", description = "Raw material not found in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public List<ReceiptResource> getBatches(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
            @PathVariable Long rawMaterialId,
            @RequestParam(required = false) @Parameter(description = "Only lots available for consumption", example = "true") Boolean usable) {
        var today = today();
        return queries.handle(new GetRawMaterialBatchesQuery(laboratoryId, environmentId, rawMaterialId)).stream()
                .filter(batch -> usable == null || batch.isUsableOn(today) == usable)
                .map(this::toBatchResource).toList();
    }

    @GetMapping("/raw-materials/{rawMaterialId}/batches/{rawMaterialBatchId}")
    @Operation(summary = "Get a raw material lot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lot found", content = @Content(schema = @Schema(implementation = ReceiptResource.class))),
            @ApiResponse(responseCode = "404", description = "Lot not found for the raw material", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                      @PathVariable Long rawMaterialId, @PathVariable Long rawMaterialBatchId) {
        return queries.handle(new GetRawMaterialBatchByIdQuery(laboratoryId, environmentId, rawMaterialId, rawMaterialBatchId))
                .<ResponseEntity<?>>map(batch -> ResponseEntity.ok(toBatchResource(batch)))
                .orElseGet(() -> notFound("RawMaterialBatch", rawMaterialBatchId));
    }

    @GetMapping("/raw-materials/{rawMaterialId}/batches/{rawMaterialBatchId}/container-assignment")
    @Operation(summary = "Get the container of a raw material lot",
            description = "Monitored container and environment where the lot is stored (US44, TS30).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Container of the lot", content = @Content(schema = @Schema(implementation = ContainerAssignmentResource.class))),
            @ApiResponse(responseCode = "404", description = "Lot not found, or the lot has no container", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getContainerAssignment(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                    @PathVariable Long rawMaterialId, @PathVariable Long rawMaterialBatchId) {
        return queries.handle(new GetRawMaterialBatchContainerQuery(laboratoryId, environmentId, rawMaterialId, rawMaterialBatchId))
                .<ResponseEntity<?>>map(container -> ResponseEntity.ok(ContainerAssignmentResourceFromEntityAssembler.toResourceFromEntity(container)))
                .orElseGet(() -> notFound("ContainerAssignment", rawMaterialBatchId));
    }

    @PutMapping(value = "/raw-materials/{rawMaterialId}/batches/{rawMaterialBatchId}/container-assignment", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Store a raw material lot in a container",
            description = "Stores the lot in an operational container monitor located in the environment of the raw material, "
                    + "which must be a raw material storage area, replacing the previous container (US43, TS29). The change is "
                    + "kept as a STORAGE movement of the lot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lot stored in the container", content = @Content(schema = @Schema(implementation = ContainerAssignmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing container monitor", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "Lot not found for the raw material", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Not a raw material storage environment, container not located there or not operational", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> assignContainer(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @PathVariable Long rawMaterialId, @PathVariable Long rawMaterialBatchId,
                                             @RequestBody AssignContainerResource resource) {
        var command = AssignRawMaterialBatchContainerCommandFromResourceAssembler.toCommandFromResource(
                laboratoryId, environmentId, rawMaterialId, rawMaterialBatchId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(commands.handle(command),
                ContainerAssignmentResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @PostMapping(value = "/raw-materials/{rawMaterialId}/batches/{rawMaterialBatchId}/reviews", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Review a raw material lot",
            description = "Registers the quality decision RELEASED, OBSERVED or REJECTED with a mandatory reason. Invalid transitions answer 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review registered", content = @Content(schema = @Schema(implementation = RawMaterialBatchReviewResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing status or reason", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing quality role"),
            @ApiResponse(responseCode = "404", description = "Lot not found for the raw material", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Transition not allowed for the current status", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> reviewBatch(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                         @PathVariable Long rawMaterialId, @PathVariable Long rawMaterialBatchId,
                                         @Valid @RequestBody ReviewRawMaterialBatchResource resource) {
        var command = ReviewRawMaterialBatchCommandFromResourceAssembler.toCommandFromResource(
                laboratoryId, environmentId, rawMaterialId, rawMaterialBatchId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(commands.handle(command),
                RawMaterialBatchReviewResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @GetMapping("/raw-materials/{rawMaterialId}/movements")
    @Operation(summary = "List the stock and review movements of a raw material",
            description = "Append-only history, newest first, including the product batch that consumed each amount.")
    public List<InventoryMovementResource> getMovements(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                        @PathVariable Long rawMaterialId) {
        return queries.handle(new GetRawMaterialMovementsQuery(laboratoryId, environmentId, rawMaterialId)).stream()
                .map(InventoryMovementResourceFromEntityAssembler::toResourceFromEntity).toList();
    }

    @GetMapping("/raw-material-batches")
    @Operation(summary = "List the raw material lots of an environment",
            description = "Use expirationStatus=NEAR_EXPIRY to list lots with available stock that expire within the near expiry period "
                    + "(withinDays or the configured default, 30 days unless overridden). Rejected and depleted lots are excluded from the filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lots of the environment ordered by expiration date",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReceiptResource.class)))),
            @ApiResponse(responseCode = "400", description = "Unsupported expirationStatus or withinDays", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public List<ReceiptResource> getEnvironmentBatches(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
            @RequestParam(required = false) @Parameter(description = "Expiration classification filter", example = "NEAR_EXPIRY") ExpirationStatus expirationStatus,
            @RequestParam(required = false) @Parameter(description = "Near expiry period in days", example = "30") Integer withinDays) {
        var period = withinDays == null ? nearExpiryPeriod : new NearExpiryPeriod(withinDays);
        var today = today();
        return queries.handle(new GetEnvironmentRawMaterialBatchesQuery(laboratoryId, environmentId, expirationStatus, withinDays))
                .stream().map(batch -> ReceiptResourceFromEntityAssembler.toResourceFromEntity(batch, today, period)).toList();
    }

    @PostMapping(value = "/raw-material-imports", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Import a legacy raw material into the environment",
            description = "Creates the raw material from a pre-Inventory record once, with its opening balance pending quality review.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raw material imported; Location points to the raw material",
                    content = @Content(schema = @Schema(implementation = RawMaterialResource.class))),
            @ApiResponse(responseCode = "404", description = "Environment or legacy record not found", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Legacy record requires reconciliation or its code already exists", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> importRawMaterial(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                               @Valid @RequestBody ImportRawMaterialResource resource) {
        var materialId = imports.handle(new ImportLegacyRawMaterialCommand(laboratoryId, environmentId, resource.legacyId()));
        var material = queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, materialId))
                .orElseThrow(() -> new ApplicationException(ApplicationError.notFound("Material", materialId)));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/raw-materials/{rawMaterialId}")
                .buildAndExpand(laboratoryId, environmentId, materialId).toUri();
        return ResponseEntity.created(location).body(RawMaterialResourceFromEntityAssembler.toResourceFromEntity(material));
    }

    private MaterialStockSummary summary(Long laboratoryId, Long environmentId, RawMaterial material) {
        return queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, material.getId()))
                .orElseThrow(() -> new ApplicationException(ApplicationError.notFound("Material", material.getId())));
    }

    private ReceiptResource toBatchResource(RawMaterialBatch batch) {
        return ReceiptResourceFromEntityAssembler.toResourceFromEntity(batch, today(), nearExpiryPeriod);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static ResponseEntity<?> notFound(String resource, Long id) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound(resource, id));
    }
}
