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

@Service
public class InventoryContextFacadeImpl implements InventoryContextFacade {
    private final InventoryCommandService commands;
    private final InventoryQueryService queries;
    private final InventoryRepository repository;
    private final Clock clock;
    private final NearExpiryPeriod nearExpiryPeriod;

    public InventoryContextFacadeImpl(InventoryCommandService commands, InventoryQueryService queries, InventoryRepository repository,
                                      Clock inventoryClock, NearExpiryPeriod nearExpiryPeriod) {
        this.commands = commands;
        this.queries = queries;
        this.repository = repository;
        this.clock = inventoryClock;
        this.nearExpiryPeriod = nearExpiryPeriod;
    }

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

    public boolean isRawMaterialInEnvironment(Long laboratoryId, Long environmentId, Long rawMaterialId) {
        return queries.handle(new GetEnvironmentRawMaterialByIdQuery(laboratoryId, environmentId, rawMaterialId)).isPresent();
    }

    public Optional<Long> findRawMaterialEnvironment(Long laboratoryId, Long rawMaterialId) {
        if (laboratoryId == null || rawMaterialId == null) return Optional.empty();
        return repository.material(laboratoryId, rawMaterialId, false).map(RawMaterial::getEnvironmentId);
    }

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
}
