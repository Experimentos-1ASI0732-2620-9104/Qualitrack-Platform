package com.iotech.qualitrack.platform.batch.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStoredInContainerEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchStoredInContainerIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes the storage of product batches in monitored containers to other bounded contexts.
 */
@Service
public class BatchStoredInContainerEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    public BatchStoredInContainerEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(BatchStoredInContainerEvent.class)
    public void on(BatchStoredInContainerEvent event) {
        eventPublisher.publishEvent(BatchStoredInContainerIntegrationEvent.from(event));
    }
}
