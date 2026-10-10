package com.iotech.qualitrack.platform.tracking.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalProfileUpdatedEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalProfileUpdatedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes profile changes so Reporting &amp; Audit records who changed the configuration.
 */
@Service
@Slf4j
public class EnvironmentalProfileUpdatedEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public EnvironmentalProfileUpdatedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(EnvironmentalProfileUpdatedEvent.class)
    public void on(EnvironmentalProfileUpdatedEvent event) {
        log.info("Environmental profile {} updated to version {}", event.profileId(), event.version());
        eventPublisher.publishEvent(EnvironmentalProfileUpdatedIntegrationEvent.from(event));
    }
}
