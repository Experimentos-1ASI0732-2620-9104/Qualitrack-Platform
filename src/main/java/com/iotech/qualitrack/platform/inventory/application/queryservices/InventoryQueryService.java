package com.iotech.qualitrack.platform.inventory.application.queryservices;

import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialMovementsQuery;
import java.util.Optional;import com.iotech.qualitrack.platform.inventory.domain.model.queries.*;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import java.util.List;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LegacyInventoryFacade;

public interface InventoryQueryService {
    List<LegacyInventoryFacade.Material> handle(GetPendingLegacyMaterialsQuery query);
    List<MaterialStockSummary> handle(GetEnvironmentRawMaterialsQuery query);
    Optional<MaterialStockSummary> handle(GetEnvironmentRawMaterialByIdQuery query);
    List<RawMaterialBatch> handle(GetRawMaterialBatchesQuery query);
    Optional<RawMaterialBatch> handle(GetRawMaterialBatchByIdQuery query);
    List<InventoryMovement> handle(GetRawMaterialMovementsQuery query);
    List<RawMaterialBatch> handle(GetEnvironmentRawMaterialBatchesQuery query);
    /** Container where the lot is stored; empty when it has no container (US44, TS30). */
    Optional<RawMaterialBatchContainer> handle(GetRawMaterialBatchContainerQuery query);
}
