package com.iotech.qualitrack.platform.batch.interfaces.rest;

import com.iotech.qualitrack.platform.batch.application.commandservices.ProductCommandService;
import com.iotech.qualitrack.platform.batch.application.queryservices.ProductQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductsByEnvironmentQuery;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.CreateProductResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.PharmaceuticalProductResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.CreateProductCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.batch.interfaces.rest.transform.ProductResourceFromEntityAssembler;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for the pharmaceutical products of an environment (TS61, TS62).
 *
 * <p>Tenant isolation of {@code laboratoryId}, {@code environmentId} and {@code productId} is enforced by
 * the IAM tenant interceptor. Registering products requires a quality role (US71).</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/products", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentProductsController {

    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;

    public EnvironmentProductsController(ProductCommandService productCommandService, ProductQueryService productQueryService) {
        this.productCommandService = productCommandService;
        this.productQueryService = productQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Register a product", description = "Registers a pharmaceutical product manufactured in the environment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product registered",
                    content = @Content(schema = @Schema(implementation = PharmaceuticalProductResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Code or name already used in the laboratory", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerProduct(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @Valid @RequestBody CreateProductResource resource) {
        var command = CreateProductCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, resource);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(productCommandService.handle(command),
                ProductResourceFromEntityAssembler::toResourceFromEntity, PharmaceuticalProduct::getId);
    }

    @GetMapping
    @Operation(summary = "List products", description = "Pharmaceutical products registered in the environment, by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products of the environment",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PharmaceuticalProductResource.class)))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account")
    })
    public ResponseEntity<List<PharmaceuticalProductResource>> getProducts(@PathVariable Long laboratoryId, @PathVariable Long environmentId) {
        return ResponseEntity.ok(productQueryService.handle(new GetProductsByEnvironmentQuery(laboratoryId, environmentId)).stream()
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product", description = "One pharmaceutical product of the environment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = PharmaceuticalProductResource.class))),
            @ApiResponse(responseCode = "403", description = "Product not available to the account"),
            @ApiResponse(responseCode = "404", description = "Product not registered in the environment", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getProduct(@PathVariable Long laboratoryId, @PathVariable Long environmentId, @PathVariable Long productId) {
        return productQueryService.handle(new GetProductByIdQuery(laboratoryId, environmentId, productId))
                .<ResponseEntity<?>>map(product -> ResponseEntity.ok(ProductResourceFromEntityAssembler.toResourceFromEntity(product)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("PharmaceuticalProduct", productId)));
    }
}
