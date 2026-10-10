package com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Outbound ACL used by Product Batch Management to reach the raw materials owned by Inventory.
 */
@Service
public class BatchExternalInventoryService {

    private final InventoryContextFacade inventoryContextFacade;

    public BatchExternalInventoryService(InventoryContextFacade inventoryContextFacade) {
        this.inventoryContextFacade = inventoryContextFacade;
    }

    /**
     * Checks that the raw material belongs to the laboratory and is kept in the environment.
     */
    public boolean isRawMaterialInEnvironment(Long laboratoryId, Long environmentId, Long rawMaterialId) {
        return inventoryContextFacade.isRawMaterialInEnvironment(laboratoryId, environmentId, rawMaterialId);
    }

    /**
     * Consumes stock of a raw material lot for a product batch.
     *
     * @throws com.iotech.qualitrack.platform.shared.application.result.ApplicationException when Inventory rejects
     * the consumption
     */
    public void consume(Long laboratoryId, Long rawMaterialBatchId, Long productBatchId, BigDecimal amountUsed,
                        String unit, String operationId) {
        inventoryContextFacade.consume(new InventoryContextFacade.ConsumptionRequest(laboratoryId, rawMaterialBatchId,
                productBatchId, amountUsed, unit, operationId));
    }

    /**
     * Finds the environment where an Inventory raw material is kept, to address it in the client.
     */
    public Optional<Long> findRawMaterialEnvironment(Long laboratoryId, Long rawMaterialId) {
        return inventoryContextFacade.findRawMaterialEnvironment(laboratoryId, rawMaterialId);
    }
}
