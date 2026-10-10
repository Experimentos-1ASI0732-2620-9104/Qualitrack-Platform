package com.iotech.qualitrack.platform.batch.domain.model.events;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;

/**
 * Domain event raised when a pharmaceutical product is registered.
 */
public record ProductCreatedEvent(
        Long productId,
        Long laboratoryId,
        Long environmentId,
        String code,
        String name
) {
    public static ProductCreatedEvent from(PharmaceuticalProduct product) {
        return new ProductCreatedEvent(product.getId(), product.getLaboratoryId(), product.getEnvironmentId(),
                product.getCode(), product.getName());
    }
}
