package com.iotech.qualitrack.platform.inventory.domain.repositories;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    List<RawMaterial> materials(Long laboratoryId);
    List<RawMaterial> materials(Long laboratoryId, Long environmentId);
    Optional<RawMaterial> materialById(Long id);
    Optional<RawMaterial> material(Long laboratoryId, Long id, boolean lock);
    RawMaterial saveMaterial(RawMaterial material, Long legacyId);
    Optional<Long> legacyId(Long laboratoryId, Long materialId);
    Optional<Long> importedMaterial(Long laboratoryId, Long legacyId);
    boolean codeExists(Long laboratoryId, String code, Long exceptId);
    List<RawMaterialBatch> receipts(Long laboratoryId, Long materialId);
    Optional<RawMaterialBatch> receipt(Long laboratoryId, Long id, boolean lock);
    Optional<RawMaterialBatch> receiptById(Long id);
    List<RawMaterialBatch> environmentReceipts(Long laboratoryId, Long environmentId);
    RawMaterialBatch saveReceipt(RawMaterialBatch receipt);
    boolean receiptExists(Long laboratoryId, Long materialId, String supplier, String batchNumber);
    List<InventoryMovement> movements(Long laboratoryId, Long materialId);
    Optional<InventoryMovement> operation(Long laboratoryId, String operationId);
    InventoryMovement append(InventoryMovement movement);
}
