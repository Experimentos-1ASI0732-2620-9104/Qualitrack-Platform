package com.iotech.qualitrack.platform.batch.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchParticipationRegisteredEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchParticipationRegisteredIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes equipment and staff associations of product batches to other bounded contexts.
 */
@Service
public class BatchParticipationRegisteredEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    public BatchParticipationRegisteredEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(BatchParticipationRegisteredEvent.class)
    public void on(BatchParticipationRegisteredEvent event) {
        eventPublisher.publishEvent(BatchParticipationRegisteredIntegrationEvent.from(event));
    }
}
