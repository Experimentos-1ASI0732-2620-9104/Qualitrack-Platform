package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.assemblers;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMovementPersistenceEntity;

/**
 * Maps inventory movements between the domain and persistence layers.
 *
 * <p>Preserves movement quantities, stock balances, operation
 * identifiers, responsible users and occurrence timestamps.</p>
 *
 * <p>Movements provide traceability for receipt, review,
 * consumption, storage and opening balance operations.</p>
 */
public final class InventoryMovementPersistenceAssembler {
    private InventoryMovementPersistenceAssembler() { }

    /**
     * Reconstructs an inventory movement from persisted data.
     *
     * @param entity persisted inventory movement
     * @return reconstructed domain movement
     */
    public static InventoryMovement toDomainFromPersistence(InventoryMovementPersistenceEntity entity) {
        return new InventoryMovement(entity.getId(), entity.getLaboratoryId(), entity.getMaterialId(), entity.getReceiptId(),
            entity.getProductBatchId(), entity.getType(), entity.getAmount(), entity.getUnit(), entity.getStockBefore(),
            entity.getStockAfter(), entity.getStatusBefore(), entity.getStatusAfter(), entity.getReason(),
            entity.getActorId(), entity.getOccurredAt(), entity.getOperationId());
    }

    /**
     * Converts an inventory movement into a JPA persistence entity.
     *
     * <p>The database generates the identifier for new movements.</p>
     *
     * @param movement domain inventory movement
     * @return persistence entity containing movement information
     */
    public static InventoryMovementPersistenceEntity toPersistenceFromDomain(InventoryMovement movement) {
        var entity = new InventoryMovementPersistenceEntity();
        entity.setLaboratoryId(movement.laboratoryId());
        entity.setMaterialId(movement.materialId());
        entity.setReceiptId(movement.receiptId());
        entity.setProductBatchId(movement.productBatchId());
        entity.setType(movement.type());
        entity.setAmount(movement.amount());
        entity.setUnit(movement.unit());
        entity.setStockBefore(movement.stockBefore());
        entity.setStockAfter(movement.stockAfter());
        entity.setStatusBefore(movement.statusBefore());
        entity.setStatusAfter(movement.statusAfter());
        entity.setReason(movement.reason());
        entity.setActorId(movement.actorId());
        entity.setOccurredAt(movement.occurredAt());
        entity.setOperationId(movement.operationId());
        return entity;
    }
}
