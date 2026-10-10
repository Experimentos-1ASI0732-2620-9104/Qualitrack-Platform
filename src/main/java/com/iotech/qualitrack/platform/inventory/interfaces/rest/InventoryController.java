package com.iotech.qualitrack.platform.inventory.interfaces.rest;

import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetPendingLegacyMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.LegacyMaterialResource;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.transform.LegacyMaterialResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Laboratory-wide view of the raw material records created before Inventory Management existed.
 *
 * <p>Raw materials, lots, reviews and movements are managed per environment through
 * {@link EnvironmentInventoryController}; consumption is registered by Product Batch Management.
 * This endpoint only previews the legacy records that can still be imported into an environment.</p>
 */
@RestController
@RequestMapping("/api/v1/laboratories/{laboratoryId}/inventory")
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class InventoryController {
    private final InventoryQueryService queries;

    public InventoryController(InventoryQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/legacy-materials")
    @Operation(summary = "Preview pre-Inventory records pending explicit import into an environment")
    public List<LegacyMaterialResource> pending(@PathVariable Long laboratoryId) {
        return queries.handle(new GetPendingLegacyMaterialsQuery(laboratoryId)).stream()
                .map(LegacyMaterialResourceFromEntityAssembler::toResourceFromEntity).toList();
    }
}
