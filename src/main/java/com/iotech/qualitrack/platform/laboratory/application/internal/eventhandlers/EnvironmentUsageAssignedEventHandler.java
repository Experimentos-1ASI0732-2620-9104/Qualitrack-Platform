package com.iotech.qualitrack.platform.laboratory.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUsageAssignedEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentUsageAssignedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles internal EnvironmentUsageAssignedEvent events and publishes the corresponding integration event.
 */
@Service
@Slf4j
public class EnvironmentUsageAssignedEventHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EnvironmentUsageAssignedEventHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(EnvironmentUsageAssignedEvent.class)
    public void on(EnvironmentUsageAssignedEvent event) {
        log.info("Environment usage assigned. environmentId={}, laboratoryId={}", event.environmentId(), event.laboratoryId());
        applicationEventPublisher.publishEvent(EnvironmentUsageAssignedIntegrationEvent.from(event));
    }
}
