package com.iotech.qualitrack.platform.inventory.interfaces.acl;

import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves the laboratory that owns Inventory resources referenced in request paths.
 *
 * <p>{@code rawMaterialId} identifies an Inventory raw material and {@code rawMaterialBatchId} one of its lots;
 * product batches keep the {@code batchId} key owned by Product Batch Management.</p>
 */
@Component
public class InventoryTenantResourceLookup implements TenantResourceLookup {
    private final InventoryRepository inventory;

    public InventoryTenantResourceLookup(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Override
    public Set<String> types() {
        return Set.of("rawMaterialId", "rawMaterialBatchId");
    }

    @Override
    public Optional<ResourceOwner> owner(String type, Long id) {
        return switch (type) {
            case "rawMaterialId" -> inventory.materialById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            case "rawMaterialBatchId" -> inventory.receiptById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            default -> Optional.empty();
        };
    }
}
