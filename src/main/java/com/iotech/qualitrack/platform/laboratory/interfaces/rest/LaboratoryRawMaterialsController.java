package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.laboratory.application.queryservices.RawMaterialQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetLowStockMaterialsByLabIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetRawMaterialsByLabIdQuery;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.RawMaterialResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.RawMaterialResourceFromEntityAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Read-only view of the raw material balances registered before Inventory Management existed.
 *
 * <p>New raw materials and lots are managed per environment by Inventory Management.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/raw-materials", produces = APPLICATION_JSON_VALUE)
public class LaboratoryRawMaterialsController {

    private final RawMaterialQueryService rawMaterialQueryService;

    public LaboratoryRawMaterialsController(RawMaterialQueryService rawMaterialQueryService) {
        this.rawMaterialQueryService = rawMaterialQueryService;
    }

    @GetMapping
    public ResponseEntity<List<RawMaterialResource>> getRawMaterials(
            @PathVariable Long laboratoryId,
            @RequestParam(required = false, defaultValue = "false") boolean lowStock
    ) {
        var materials = lowStock
                ? rawMaterialQueryService.handle(new GetLowStockMaterialsByLabIdQuery(laboratoryId))
                : rawMaterialQueryService.handle(new GetRawMaterialsByLabIdQuery(laboratoryId));

        var resources = materials.stream()
                .map(RawMaterialResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }
}
