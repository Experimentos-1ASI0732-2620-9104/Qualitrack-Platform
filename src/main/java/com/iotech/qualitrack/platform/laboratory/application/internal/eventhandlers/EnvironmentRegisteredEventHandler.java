package com.iotech.qualitrack.platform.laboratory.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentRegisteredEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentRegisteredIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles internal EnvironmentRegisteredEvent events and publishes the corresponding integration event.
 */
@Service
@Slf4j
public class EnvironmentRegisteredEventHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EnvironmentRegisteredEventHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(EnvironmentRegisteredEvent.class)
    public void on(EnvironmentRegisteredEvent event) {
        log.info("Environment registered. environmentId={}, laboratoryId={}", event.environmentId(), event.laboratoryId());
        applicationEventPublisher.publishEvent(EnvironmentRegisteredIntegrationEvent.from(event));
    }
}
