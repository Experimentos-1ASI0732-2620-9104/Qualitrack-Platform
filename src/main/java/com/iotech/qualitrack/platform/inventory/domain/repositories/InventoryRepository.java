package com.iotech.qualitrack.platform.inventory.domain.repositories;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import java.util.List;
import java.util.Optional;

/**
 * Defines persistence operations required by the Inventory
 * bounded context.
 *
 * <p>Provides access to raw materials, supplier lots
 * and inventory movements while keeping the domain
 * independent of persistence technologies.</p>
 *
 * <p>Laboratory identifiers are used to restrict
 * inventory operations to the corresponding laboratory.</p>
 */
public interface InventoryRepository {

    /**
     * Retrieves the raw materials of a laboratory.
     *
     * @param laboratoryId the laboratory identifier
     * @return the registered raw materials
     */
    List<RawMaterial> materials(Long laboratoryId);

    /**
     * Retrieves raw materials within a specific environment.
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @return the materials registered in that environment
     */
    List<RawMaterial> materials(Long laboratoryId, Long environmentId);

    /**
     * Searches for a raw material by its identifier.
     *
     * @param id the material identifier
     * @return the material, if present
     */
    Optional<RawMaterial> materialById(Long id);

    /**
     * Retrieves a laboratory-owned raw material.
     *
     * @param laboratoryId the laboratory identifier
     * @param id the material identifier
     * @param lock whether to request a persistence lock
     * @return the material, if present
     */
    Optional<RawMaterial> material(Long laboratoryId, Long id, boolean lock);

    /**
     * Persists a raw material and its optional legacy reference.
     *
     * @param material the raw material to persist
     * @param legacyId the legacy material identifier, if applicable
     * @return the persisted material
     */
    RawMaterial saveMaterial(RawMaterial material, Long legacyId);

    /**
     * Finds the legacy identifier associated with a material.
     *
     * @param laboratoryId the laboratory identifier
     * @param materialId the material identifier
     * @return the legacy identifier, if present
     */
    Optional<Long> legacyId(Long laboratoryId, Long materialId);

    /**
     * Finds a material previously imported from legacy inventory.
     *
     * @param laboratoryId the laboratory identifier
     * @param legacyId the original legacy material identifier
     * @return the imported material identifier, if present
     */
    Optional<Long> importedMaterial(Long laboratoryId, Long legacyId);

    /**
     * Checks whether a material code is already registered
     * within a laboratory.
     *
     * @param laboratoryId the laboratory identifier
     * @param code the material code
     * @param exceptId material identifier excluded from the check
     * @return true when another material uses the code
     */
    boolean codeExists(Long laboratoryId, String code, Long exceptId);

    /**
     * Retrieves supplier lots belonging to a raw material.
     *
     * @param laboratoryId the laboratory identifier
     * @param materialId the material identifier
     * @return the supplier lots of the material
     */
    List<RawMaterialBatch> receipts(Long laboratoryId, Long materialId);

    /**
     * Retrieves a supplier lot belonging to a laboratory.
     *
     * @param laboratoryId the laboratory identifier
     * @param id the supplier lot identifier
     * @param lock whether to request a persistence lock
     * @return the supplier lot, if present
     */
    Optional<RawMaterialBatch> receipt(Long laboratoryId, Long id, boolean lock);

    /**
     * Searches for a supplier lot by its identifier.
     *
     * @param id the supplier lot identifier
     * @return the supplier lot, if present
     */
    Optional<RawMaterialBatch> receiptById(Long id);

    /**
     * Retrieves supplier lots registered within an environment.
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @return the supplier lots of the environment
     */
    List<RawMaterialBatch> environmentReceipts(Long laboratoryId, Long environmentId);

    /**
     * Persists a supplier lot and its current state.
     *
     * @param receipt the supplier lot to persist
     * @return the persisted supplier lot
     */
    RawMaterialBatch saveReceipt(RawMaterialBatch receipt);

    /**
     * Checks whether a supplier lot is already registered.
     *
     * @param laboratoryId the laboratory identifier
     * @param materialId the material identifier
     * @param supplier the supplier name
     * @param batchNumber the supplier lot number
     * @return true when a matching supplier lot exists
     */
    boolean receiptExists(Long laboratoryId, Long materialId, String supplier, String batchNumber);

    /**
     * Retrieves inventory movements of a raw material.
     *
     * @param laboratoryId the laboratory identifier
     * @param materialId the material identifier
     * @return the movement history of the material
     */
    List<InventoryMovement> movements(Long laboratoryId, Long materialId);

    /**
     * Finds an inventory movement by its operation identifier.
     *
     * <p>Supports identifying previously registered operations
     * to prevent duplicate inventory consumption.</p>
     *
     * @param laboratoryId the laboratory identifier
     * @param operationId the operation identifier
     * @return the existing movement, if present
     */
    Optional<InventoryMovement> operation(Long laboratoryId, String operationId);

    /**
     * Records a new inventory movement.
     *
     * @param movement the movement to persist
     * @return the persisted movement
     */
    InventoryMovement append(InventoryMovement movement);
}
