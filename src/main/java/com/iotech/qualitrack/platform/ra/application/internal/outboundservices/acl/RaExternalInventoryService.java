package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anti-corruption service that reads, through Inventory Management, the raw materials and lots of an environment.
 */
@Service
public class RaExternalInventoryService {
    private final InventoryContextFacade inventoryContextFacade;

    public RaExternalInventoryService(InventoryContextFacade inventoryContextFacade) {
        this.inventoryContextFacade = inventoryContextFacade;
    }

    /**
     * Raw materials of an environment with their stock and lots, ordered by code.
     */
    public List<InventoryContextFacade.InventoryMaterial> findInventory(Long laboratoryId, Long environmentId) {
        return inventoryContextFacade.findInventory(laboratoryId, environmentId);
    }
}
