package com.iotech.qualitrack.platform.laboratory.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUpdatedEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentUpdatedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles internal EnvironmentUpdatedEvent events and publishes the corresponding integration event.
 */
@Service
@Slf4j
public class EnvironmentUpdatedEventHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EnvironmentUpdatedEventHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(EnvironmentUpdatedEvent.class)
    public void on(EnvironmentUpdatedEvent event) {
        log.info("Environment updated. environmentId={}, laboratoryId={}", event.environmentId(), event.laboratoryId());
        applicationEventPublisher.publishEvent(EnvironmentUpdatedIntegrationEvent.from(event));
    }
}
