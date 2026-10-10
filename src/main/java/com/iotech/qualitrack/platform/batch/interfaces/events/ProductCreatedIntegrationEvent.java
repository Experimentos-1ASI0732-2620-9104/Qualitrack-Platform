package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.ProductCreatedEvent;

/**
 * Integration event published to other bounded contexts when a pharmaceutical product is registered.
 */
public record ProductCreatedIntegrationEvent(
        Long productId,
        Long laboratoryId,
        Long environmentId,
        String code,
        String name
) {
    public static ProductCreatedIntegrationEvent from(ProductCreatedEvent event) {
        return new ProductCreatedIntegrationEvent(event.productId(), event.laboratoryId(), event.environmentId(),
                event.code(), event.name());
    }
}
