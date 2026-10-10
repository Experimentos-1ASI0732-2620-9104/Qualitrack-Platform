package com.iotech.qualitrack.platform.inventory.application.internal.queryservices;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalEquipmentService;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetEnvironmentRawMaterialsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchByIdQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialMovementsQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import java.util.Optional;import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.*;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.shared.application.result.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LegacyInventoryFacade;

@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {
    private final InventoryRepository repository;
    private final Clock clock;
    private final LegacyInventoryFacade legacy;
    private final InventoryExternalLaboratoryService laboratories;
    private final NearExpiryPeriod nearExpiryPeriod;
    private final InventoryExternalEquipmentService equipment;
    public InventoryQueryServiceImpl(InventoryRepository repository, Clock inventoryClock, LegacyInventoryFacade legacy,
            InventoryExternalLaboratoryService laboratories, NearExpiryPeriod inventoryNearExpiryPeriod,
            InventoryExternalEquipmentService equipment) {
        this.equipment = equipment;
        this.repository = repository;
        this.laboratories = laboratories;
        this.nearExpiryPeriod = inventoryNearExpiryPeriod;
        this.clock = inventoryClock;
        this.legacy = legacy;
    }
    @Override
    public List<LegacyInventoryFacade.Material> handle(GetPendingLegacyMaterialsQuery query) {
        return legacy.materials(query.laboratoryId()).stream()
            .filter(material -> repository.importedMaterial(query.laboratoryId(), material.id()).isEmpty()).toList();
    }

    public List<MaterialStockSummary> handle(GetEnvironmentRawMaterialsQuery query) {
        requireEnvironment(query.laboratoryId(), query.environmentId());
        return repository.materials(query.laboratoryId(), query.environmentId()).stream().map(this::summary)
            .filter(material -> query.stockStatus() == null || material.stockStatus() == query.stockStatus()).toList();
    }
    public Optional<MaterialStockSummary> handle(GetEnvironmentRawMaterialByIdQuery query) {
        return repository.material(query.laboratoryId(), query.rawMaterialId(), false)
            .filter(material -> material.belongsToEnvironment(query.environmentId())).map(this::summary);
    }
    public List<RawMaterialBatch> handle(GetRawMaterialBatchesQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.receipts(query.laboratoryId(), query.rawMaterialId());
    }
    public Optional<RawMaterialBatch> handle(GetRawMaterialBatchByIdQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.receipt(query.laboratoryId(), query.rawMaterialBatchId(), false)
            .filter(receipt -> receipt.getRawMaterialId().equals(query.rawMaterialId()));
    }
    public List<InventoryMovement> handle(GetRawMaterialMovementsQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        return repository.movements(query.laboratoryId(), query.rawMaterialId());
    }
    public List<RawMaterialBatch> handle(GetEnvironmentRawMaterialBatchesQuery query) {
        requireEnvironment(query.laboratoryId(), query.environmentId());
        var period = query.withinDays() == null ? nearExpiryPeriod : new NearExpiryPeriod(query.withinDays());
        var today = LocalDate.now(clock);
        return repository.environmentReceipts(query.laboratoryId(), query.environmentId()).stream()
            .filter(receipt -> query.expirationStatus() == null
                || (receipt.expirationStatus(today, period.days()) == query.expirationStatus()
                    && receipt.getAvailableAmount().signum() > 0
                    && receipt.getStatus() != RawMaterialBatchStatus.REJECTED))
            .toList();
    }

    /**
     * @throws ApplicationException not found when the environment, the material or the lot do not exist
     */
    public Optional<RawMaterialBatchContainer> handle(GetRawMaterialBatchContainerQuery query) {
        requireMaterialInEnvironment(query.laboratoryId(), query.environmentId(), query.rawMaterialId());
        var receipt = repository.receipt(query.laboratoryId(), query.rawMaterialBatchId(), false)
            .filter(lot -> lot.getRawMaterialId().equals(query.rawMaterialId()))
            .orElseThrow(() -> new ApplicationException(ApplicationError.notFound("RawMaterialBatch", query.rawMaterialBatchId())));
        return receipt.container().map(container -> new RawMaterialBatchContainer(receipt.getId(), container.containerMonitorId(),
            equipment.findContainerMonitor(query.laboratoryId(), container.containerMonitorId())
                .map(EquipmentContextFacade.ContainerReference::name).orElse(null),
            container.environmentId(), container.assignedBy(), container.assignedAt()));
    }

    private MaterialStockSummary summary(RawMaterial material) {
        var lab = material.getLaboratoryId();
        var receipts = repository.receipts(lab, material.getId());
        return new MaterialStockSummary(material.getId(), lab, material.getEnvironmentId(), material.getCode(), material.getName(),
            material.getUnit(), material.getMinimumStock(), material.usableStock(receipts, LocalDate.now(clock)),
            receipts.stream().map(RawMaterialBatch::getAvailableAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
            repository.legacyId(lab, material.getId()).orElse(null));
    }

    private void requireEnvironment(Long lab, Long environment) {
        if (!laboratories.existsEnvironment(lab, environment))
            throw new ApplicationException(ApplicationError.notFound("Environment", environment));
    }

    private void requireMaterialInEnvironment(Long lab, Long environment, Long id) {
        requireEnvironment(lab, environment);
        if (repository.material(lab, id, false).filter(material -> material.belongsToEnvironment(environment)).isEmpty())
            throw new ApplicationException(ApplicationError.notFound("Material", id));
    }

}
