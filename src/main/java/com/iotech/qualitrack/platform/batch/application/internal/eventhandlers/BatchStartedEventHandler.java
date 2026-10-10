package com.iotech.qualitrack.platform.batch.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStartedEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchStartedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles internal BatchStartedEvent events and publishes the corresponding integration event.
 */
@Slf4j
@Service
public class BatchStartedEventHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Creates a new BatchStartedEventHandler.
     *
     * @param applicationEventPublisher Spring application event publisher
     */
    public BatchStartedEventHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Handles the internal batch started domain event.
     *
     * @param event the internal batch started domain event
     */
    @EventListener(BatchStartedEvent.class)
    public void on(BatchStartedEvent event) {
        log.info(
                "Batch started. batchId={}, laboratoryId={}, batchNumber={}, startedAt={}",
                event.batchId(),
                event.laboratoryId(),
                event.batchNumber(),
                event.startedAt()
        );

        applicationEventPublisher.publishEvent(BatchStartedIntegrationEvent.from(event));
    }
}
