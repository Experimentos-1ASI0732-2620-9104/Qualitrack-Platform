package com.iotech.qualitrack.platform.inventory.application.internal.queryservices;

import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalEquipmentService;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
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
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ExpirationStatus;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.LegacyRawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


/**
 * Application service responsible for handling inventory queries
 * within the Inventory bounded context.
 *
 * <p>Provides read operations for raw materials, supplier batches,
 * inventory movements, legacy materials and storage assignments.</p>
 *
 * @see InventoryQueryService
 */
@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {
    private final InventoryRepository repository;
    private final Clock clock;
    private final InventoryExternalLaboratoryService laboratories;
    private final NearExpiryPeriod nearExpiryPeriod;
    private final InventoryExternalEquipmentService equipment;
    public InventoryQueryServiceImpl(InventoryRepository repository, Clock inventoryClock,
            InventoryExternalLaboratoryService laboratories, NearExpiryPeriod inventoryNearExpiryPeriod,
            InventoryExternalEquipmentService equipment) {
        this.equipment = equipment;
        this.repository = repository;
        this.laboratories = laboratories;
        this.nearExpiryPeriod = inventoryNearExpiryPeriod;
        this.clock = inventoryClock;
    }

    /**
     * Retrieves legacy raw materials that have not yet been imported
     * into the current Inventory bounded context.
     *
     * @param query query identifying the laboratory whose legacy
     *              materials will be retrieved
     * @return a list of legacy raw materials pending import
     */
    @Override
    public List<LegacyRawMaterial> handle(GetPendingLegacyMaterialsQuery query) {
        return laboratories.findLegacyMaterials(query.laboratoryId()).stream()
            .filter(material -> repository.importedMaterial(query.laboratoryId(), material.id()).isEmpty()).toList();
    }

    /**
     * Retrieves raw materials registered within a laboratory environment.
     *
     * <p>Validates the environment and generates a stock summary
     * for each registered material.</p>
     *
     * @param query query containing the laboratory, environment
     *              and optional stock status filter
     * @return a list of material stock summaries matching the criteria
     * @throws ApplicationException when the specified environment
     *                              cannot be found
     */
    @Override
    public List<MaterialStockSummary> handle(GetEnvironmentRawMaterialsQuery query) {
        requireEnvironment(query.laboratoryId(), query.environmentId());
        LocalDate referenceDate = LocalDate.now(clock);
        return repository.materials(query.laboratoryId(), query.environmentId()).stream()
                .map(material -> summary(material,referenceDate))
                .filter(material -> query.stockStatus() == null || material.stockStatus() == query.stockStatus())
                .toList();
    }

    /**
     * Retrieves the stock summary of a specific raw material
     * within a laboratory environment.
     *
     * <p>Searches for the material using its laboratory and material
     * identifiers, then verifies that it belongs to the requested
     * environment.</p>
     *
     * @param query query identifying the laboratory, environment
     *              and raw material
     * @return an optional containing the material stock summary,
     *         or an empty optional when no matching material exists
     */
    @Override
    public Optional<MaterialStockSummary> handle(GetEnvironmentRawMaterialByIdQuery query) {
        LocalDate referenceDate = LocalDate.now(clock);
        return repository.material(query.laboratoryId(), query.rawMaterialId(), false)
                .filter(material -> material.belongsToEnvironment(query.environmentId()))
                .map(material -> summary(material, referenceDate));
    }

    /**
     * Retrieves supplier batches associated with a raw material.
     *
     * <p>Verifies that the material belongs to the specified
     * laboratory environment before retrieving its registered
     * supplier batches.</p>
     *
     * @param query query identifying the laboratory, environment
     *              and raw material
     * @return a list of supplier batches associated with the material
     * @throws ApplicationException when the environment or raw
     *                              material cannot be found
     */
    @Override
    public List<RawMaterialBatch> handle(GetRawMaterialBatchesQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.receipts(query.laboratoryId(), query.rawMaterialId());
    }

    /**
     * Retrieves a specific supplier batch associated with a raw material.
     *
     * <p>Validates the material's laboratory environment and verifies
     * that the requested supplier batch belongs to that material.</p>
     *
     * @param query query identifying the laboratory, environment,
     *              raw material and supplier batch
     * @return an optional containing the supplier batch when found,
     *         or an empty optional when no matching batch exists
     * @throws ApplicationException when the environment or raw
     *                              material cannot be found
     */
    @Override
    public Optional<RawMaterialBatch> handle(GetRawMaterialBatchByIdQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.receipt(query.laboratoryId(), query.rawMaterialBatchId(), false)
            .filter(receipt -> receipt.getRawMaterialId().equals(query.rawMaterialId()));
    }

    /**
     * Retrieves the inventory movement history of a raw material.
     *
     * @param query query identifying the laboratory, environment
     *              and raw material
     * @return a list of inventory movements associated with the material
     * @throws ApplicationException when the environment or raw
     *                              material cannot be found
     */
    @Override
    public List<InventoryMovement> handle(GetRawMaterialMovementsQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.movements(query.laboratoryId(), query.rawMaterialId());
    }

    /**
     * Retrieves supplier batches registered within a laboratory
     * environment, optionally filtered by expiration status.
     *
     * <p>When an expiration filter is provided, only batches
     * matching the requested expiration status, containing
     * available stock and not marked as rejected are included.</p>
     *
     * @param query query containing the laboratory, environment,
     *              optional expiration status and near-expiry period
     * @return a list of supplier batches matching the requested criteria
     * @throws ApplicationException when the specified environment
     *                              cannot be found
     */
    @Override
    public List<RawMaterialBatch> handle(GetEnvironmentRawMaterialBatchesQuery query) {
        requireEnvironment(query.laboratoryId(), query.environmentId());
        var period = query.withinDays() == null ? nearExpiryPeriod : new NearExpiryPeriod(query.withinDays());
        var today = LocalDate.now(clock);
        return repository.environmentReceipts(query.laboratoryId(), query.environmentId()).stream()
            .filter(receipt ->
                    matchesExpirationFilter(receipt,query.expirationStatus(),today, period.days()))
                .toList();
    }

    /**
     * Retrieves the storage container assignment of a supplier batch.
     *
     * <p>Validates the environment and material ownership before
     * retrieving the requested supplier batch.</p>
     *
     * <p>The container name may be {@code null} when the external
     * Equipment service cannot resolve it.</p>
     *
     * @param query query identifying the laboratory, environment,
     *              raw material and supplier batch
     * @return an optional containing the supplier batch container
     *         assignment, or an empty optional when the batch has
     *         no assigned container
     * @throws ApplicationException when the environment, raw material
     *                              or supplier batch cannot be found
     */
    @Override
    public Optional<RawMaterialBatchContainer> handle(GetRawMaterialBatchContainerQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        var receipt = repository.receipt(query.laboratoryId(), query.rawMaterialBatchId(), false)
            .filter(lot -> lot.getRawMaterialId().equals(query.rawMaterialId()))
            .orElseThrow(() -> new ApplicationException(ApplicationError.notFound("RawMaterialBatch", query.rawMaterialBatchId())));
        return receipt.container().map(container -> new RawMaterialBatchContainer(receipt.getId(), container.containerMonitorId(),
            equipment.findContainerName(query.laboratoryId(), container.containerMonitorId()).orElse(null),
            container.environmentId(), container.assignedBy(), container.assignedAt()));
    }


    /**
     * Determines whether a supplier batch matches the
     * requested expiration filter.
     *
     * <p>When no expiration status is requested, all batches
     * are included. Otherwise, only batches with available
     * stock that have not been rejected are considered.</p>
     *
     * @param receipt the supplier batch to evaluate
     * @param expirationStatus the requested expiration status
     * @param referenceDate the date used to evaluate expiration
     * @param nearExpiryDays the number of days defining near expiry
     * @return true when the batch matches the requested criteria
     */
    private boolean matchesExpirationFilter(RawMaterialBatch receipt, ExpirationStatus expirationStatus, LocalDate referenceDate, int nearExpiryDays) {
        if (expirationStatus == null) return true;

        return receipt.expirationStatus(referenceDate, nearExpiryDays) == expirationStatus
                && receipt.getAvailableAmount().signum() > 0
                && receipt.getStatus() != RawMaterialBatchStatus.REJECTED;
    }

    /**
     * Builds a stock summary for a raw material using its
     * registered supplier batches.
     *
     * @param material raw material whose stock will be summarized
     * @param referenceDate date used to evaluate usable stock
     * @return a stock summary containing material information,
     *         usable quantity, total remaining quantity and
     *         optional legacy identification
     */
    private MaterialStockSummary summary(RawMaterial material, LocalDate referenceDate) {
        var lab = material.getLaboratoryId();
        var receipts = repository.receipts(lab, material.getId());
        return new MaterialStockSummary(material.getId(), lab, material.getEnvironmentId(), material.getCode(), material.getName(),
            material.getUnit(), material.getMinimumStock(), material.usableStock(receipts, referenceDate),
            receipts.stream().map(RawMaterialBatch::getAvailableAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
            repository.legacyId(lab, material.getId()).orElse(null));
    }

    /**
     * Verifies that an environment exists within the
     * specified laboratory.
     *
     * @param lab identifier of the laboratory
     * @param environment identifier of the environment to validate
     * @throws ApplicationException when the environment cannot
     *                              be found in the laboratory
     */
    private void requireEnvironment(Long lab, Long environment) {
        if (!laboratories.existsEnvironment(lab, environment))
            throw new ApplicationException(ApplicationError.notFound("Environment", environment));
    }

    /**
     * Verifies that a raw material belongs to an existing
     * laboratory environment.
     *
     * <p>This validation prevents inventory queries from
     * accessing materials outside their assigned environment.</p>
     *
     * @param lab identifier of the laboratory
     * @param environment identifier of the environment
     * @param id identifier of the raw material to validate
     * @throws ApplicationException when the environment does not
     *                              exist or the material does not
     *                              belong to the requested environment
     */
    private void requireMaterialInEnvironment(Long lab, Long environment, Long id) {
        requireEnvironment(lab, environment);
        if (repository.material(lab, id, false).filter(material -> material.belongsToEnvironment(environment)).isEmpty())
            throw new ApplicationException(ApplicationError.notFound("Material", id));
    }

}
