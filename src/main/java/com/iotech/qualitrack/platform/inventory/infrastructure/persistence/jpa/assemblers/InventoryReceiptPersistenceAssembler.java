package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.assemblers;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ContainerAssignment;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryReceiptEntity;

public final class InventoryReceiptPersistenceAssembler {
    private InventoryReceiptPersistenceAssembler() { }
    public static RawMaterialBatch toDomainFromPersistence(InventoryReceiptEntity entity) {
        return new RawMaterialBatch(entity.getId(), entity.getLaboratoryId(), entity.getMaterialId(), entity.getSupplier(),
            entity.getBatchNumber(), entity.getUnit(), entity.getInitialAmount(), entity.getAvailableAmount(),
            entity.getReceivedOn(), entity.getExpiresOn(), entity.getStatus(),
            entity.getContainerMonitorId() == null ? null : new ContainerAssignment(entity.getContainerMonitorId(),
                entity.getContainerEnvironmentId(), entity.getContainerAssignedBy(), entity.getContainerAssignedAt()));
    }
    public static InventoryReceiptEntity toPersistenceFromDomain(RawMaterialBatch receipt) {
        var entity = new InventoryReceiptEntity();
        entity.setId(receipt.getId());
        entity.setLaboratoryId(receipt.getLaboratoryId());
        entity.setMaterialId(receipt.getRawMaterialId());
        entity.setSupplier(receipt.getSupplier());
        entity.setBatchNumber(receipt.getBatchNumber());
        entity.setUnit(receipt.getUnit());
        entity.setInitialAmount(receipt.getInitialAmount());
        entity.setAvailableAmount(receipt.getAvailableAmount());
        entity.setReceivedOn(receipt.getReceivedOn());
        entity.setExpiresOn(receipt.getExpiresOn());
        entity.setStatus(receipt.getStatus());
        receipt.container().ifPresent(container -> {
            entity.setContainerMonitorId(container.containerMonitorId());
            entity.setContainerEnvironmentId(container.environmentId());
            entity.setContainerAssignedBy(container.assignedBy());
            entity.setContainerAssignedAt(container.assignedAt());
        });
        return entity;
    }
}
