package com.iotech.qualitrack.platform.batch.interfaces.acl;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the laboratory that owns product batches and pharmaceutical products for tenant checks.
 */
@Component
public class BatchTenantResourceLookup implements TenantResourceLookup {
    private final BatchRepository batches;
    private final ProductRepository products;

    public BatchTenantResourceLookup(BatchRepository batches, ProductRepository products) {
        this.batches = batches;
        this.products = products;
    }

    public Set<String> types() { return Set.of("batchId", "productId"); }

    public Optional<ResourceOwner> owner(String type, Long id) {
        return switch (type) {
            case "batchId" -> batches.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLabId()));
            case "productId" -> products.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            default -> Optional.empty();
        };
    }
}
