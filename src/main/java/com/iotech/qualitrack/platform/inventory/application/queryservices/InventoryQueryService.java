package com.iotech.qualitrack.platform.inventory.application.queryservices;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetPendingLegacyMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchContainerQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialMovementsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.LegacyRawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;

import java.util.List;
import java.util.Optional;


/**
 * Defines the read operations available within the Inventory
 * bounded context.
 *
 * <p>Provides access to raw materials, supplier lots,
 * inventory movements, stock summaries and legacy materials.</p>
 *
 * <p>Query operations retrieve information without modifying
 * inventory stock or the lifecycle of supplier lots.</p>
 */
public interface InventoryQueryService {

    /**
     * Retrieves legacy raw materials that have not been
     * imported into the current inventory.
     *
     * @param query the laboratory whose pending materials
     *              will be retrieved
     * @return the legacy materials pending import
     */
    List<LegacyRawMaterial> handle(GetPendingLegacyMaterialsQuery query);

    /**
     * Retrieves raw materials registered in an environment.
     *
     * <p>The result includes usable stock, physical stock
     * and minimum stock. An optional stock status can
     * restrict the results.</p>
     *
     * @param query the laboratory, environment and optional
     *              stock status filter
     * @return the matching raw material stock summaries
     */
    List<MaterialStockSummary> handle(GetEnvironmentRawMaterialsQuery query);

    /**
     * Retrieves a raw material belonging to an environment.
     *
     * @param query the laboratory, environment and material IDs
     * @return the material stock summary, or an empty result
     *         when the material is not found in that environment
     */
    Optional<MaterialStockSummary> handle(GetEnvironmentRawMaterialByIdQuery query);

    /**
     * Retrieves supplier lots associated with a raw material.
     *
     * @param query the laboratory, environment and material IDs
     * @return the supplier lots registered for the material
     */
    List<RawMaterialBatch> handle(GetRawMaterialBatchesQuery query);

    /**
     * Retrieves a specific supplier lot of a raw material.
     *
     * @param query the laboratory, environment, material
     *              and supplier lot identifiers
     * @return the supplier lot, if present
     */
    Optional<RawMaterialBatch> handle(GetRawMaterialBatchByIdQuery query);

    /**
     * Retrieves movements associated with a raw material.
     *
     * <p>Movements provide traceability of changes such as
     * reception, quality review, consumption and storage.</p>
     *
     * @param query the laboratory, environment and material IDs
     * @return the inventory movements of the material
     */
    List<InventoryMovement> handle(GetRawMaterialMovementsQuery query);

    /**
     * Retrieves supplier lots registered in an environment.
     *
     * <p>Supports filtering by expiration status and a
     * configurable near-expiry period.</p>
     *
     * @param query the environment and optional expiration filters
     * @return the supplier lots matching the requested criteria
     */
    List<RawMaterialBatch> handle(GetEnvironmentRawMaterialBatchesQuery query);

    /**
     * Retrieves the monitored container assigned to a supplier lot.
     *
     * @param query the laboratory, environment, material
     *              and supplier lot identifiers
     * @return the container assignment, or an empty result
     *         when the lot has no assigned container
     */
    Optional<RawMaterialBatchContainer> handle(GetRawMaterialBatchContainerQuery query);
}
