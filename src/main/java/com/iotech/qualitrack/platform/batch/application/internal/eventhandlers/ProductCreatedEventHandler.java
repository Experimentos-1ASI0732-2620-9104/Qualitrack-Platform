package com.iotech.qualitrack.platform.batch.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.domain.model.events.ProductCreatedEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.ProductCreatedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles product creation domain events and publishes Product Batch integration events.
 */
@Service
@Slf4j
public class ProductCreatedEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates the handler with the Spring application event publisher.
     *
     * @param eventPublisher Spring application event publisher
     */
    public ProductCreatedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles pharmaceutical product creation events.
     *
     * @param event The internal Product Batch domain event.
     */
    @EventListener(ProductCreatedEvent.class)
    public void on(ProductCreatedEvent event) {
        log.info(
                "Product created: Product ID='{}', Laboratory ID='{}', Code='{}', Name='{}'.",
                event.productId(),
                event.laboratoryId(),
                event.code(),
                event.name()
        );

        eventPublisher.publishEvent(ProductCreatedIntegrationEvent.from(event));
    }
}