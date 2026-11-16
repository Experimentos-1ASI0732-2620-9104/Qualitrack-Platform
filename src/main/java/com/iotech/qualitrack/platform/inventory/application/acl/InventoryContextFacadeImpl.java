package com.iotech.qualitrack.platform.inventory.application.acl;

import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryCommandService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ConsumeRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ReceiptConsumption;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


/**
 * Implements the public integration boundary
 * of the Inventory bounded context.
 *
 * <p>Provides operations used by other bounded contexts
 * to consume raw material stock, verify material ownership
 * and retrieve inventory information.</p>
 *
 * <p>Coordinates application services and translates
 * internal inventory results into shared integration
 * contract representations.</p>
 *
 * <p>Callers do not need direct access to Inventory
 * repositories or persistence entities.</p>
 *
 * @see com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade
 */
@Service
public class InventoryContextFacadeImpl implements InventoryContextFacade {
    private final InventoryCommandService commands;
    private final InventoryQueryService queries;
    private final InventoryRepository repository;
    private final Clock clock;
    private final NearExpiryPeriod nearExpiryPeriod;


    /**
     * Initializes the Inventory integration facade.
     *
     * @param commands the inventory command service
     * @param queries the inventory query service
     * @param repository the inventory repository
     * @param inventoryClock the clock used for business dates
     * @param nearExpiryPeriod the configured expiration monitoring period
     */
    public InventoryContextFacadeImpl(InventoryCommandService commands, InventoryQueryService queries, InventoryRepository repository,
                                      Clock inventoryClock, NearExpiryPeriod nearExpiryPeriod) {
        this.commands = commands;
        this.queries = queries;
        this.repository = repository;
        this.clock = inventoryClock;
        this.nearExpiryPeriod = nearExpiryPeriod;
    }


    /**
     * Coordinates raw material consumption requested by another bounded context.
     *
     * @param request the supplier lot, product batch, quantity, unit and operation identifier
     * @return the consumption result with stock balances
     * @throws ApplicationException when the consumption operation fails
     */
    public Consumption consume(ConsumptionRequest request) {
        var result = commands.handle(new ConsumeRawMaterialBatchCommand(request.laboratoryId(), request.rawMaterialBatchId(),
                request.productBatchId(), request.amountUsed(), request.unit(), request.operationId()));
        return switch (result) {
            case Result.Success<ReceiptConsumption, ApplicationError> success -> {
                var value = success.value();
                yield new Consumption(value.rawMaterialBatchId(), value.productBatchId(), value.amountUsed(), value.unit(),
                        value.stockBefore(), value.stockAfter(), value.operationId());
            }
            case Result.Failure<ReceiptConsumption, ApplicationError> failure -> throw new ApplicationException(failure.error());
        };
    }


    /**
     * Verifies whether a raw material belongs to a specific laboratory environment.
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @param rawMaterialId the material identifier
     * @return true when the material belongs to the requested environment
     */
    public boolean isRawMaterialInEnvironment(Long laboratoryId, Long environmentId, Long rawMaterialId) {
        return queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, rawMaterialId)).isPresent();
    }


    /**
     * Retrieves the inventory of a laboratory environment
     * for consumption by other bounded contexts.
     *
     * <p>Materials are ordered by code. Supplier lots
     * are ordered by reception date.</p>
     *
     * <p>Expiration status is calculated using the
     * configured business clock and near-expiry period.</p>
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @return the inventory materials and their supplier lots,
     *         or an empty list when required identifiers are null
     */
    public List<InventoryMaterial> findInventory(Long laboratoryId, Long environmentId) {
        if (laboratoryId == null || environmentId == null) return List.of();
        var today = LocalDate.now(clock);
        return queries.handle(new GetEnvironmentRawMaterialsQuery(laboratoryId, environmentId, null)).stream()
                .sorted(Comparator.comparing(MaterialStockSummary::code))
                .map(material -> new InventoryMaterial(material.id(), material.environmentId(), material.code(), material.name(),
                        material.unit(), material.minimumStock(), material.usableStock(), material.physicalStock(),
                        material.stockStatus().name(),
                        queries.handle(new GetRawMaterialBatchesQuery(laboratoryId, environmentId, material.id())).stream()
                                .sorted(Comparator.comparing(RawMaterialBatch::getReceivedOn))
                                .map(lot -> new InventoryLot(lot.getId(), lot.getSupplier(), lot.getBatchNumber(),
                                        lot.getInitialAmount(), lot.getAvailableAmount(), lot.getReceivedOn(), lot.getExpiresOn(),
                                        lot.getStatus().name(), lot.expirationStatus(today, nearExpiryPeriod.days()).name(),
                                        lot.container().map(container -> container.containerMonitorId()).orElse(null)))
                                .toList()))
                .toList();
    }


    /**
     * Finds the environment associated with a raw material.
     *
     * <p>The lookup is restricted to the specified laboratory.</p>
     *
     * @param laboratoryId the laboratory identifier
     * @param rawMaterialId the material identifier
     * @return the environment identifier, if available
     */
    public Optional<Long> findRawMaterialEnvironment(Long laboratoryId, Long rawMaterialId) {
        if (laboratoryId == null || rawMaterialId == null) return Optional.empty();
        return repository.material(laboratoryId, rawMaterialId, false).map(RawMaterial::getEnvironmentId);
    }

}
