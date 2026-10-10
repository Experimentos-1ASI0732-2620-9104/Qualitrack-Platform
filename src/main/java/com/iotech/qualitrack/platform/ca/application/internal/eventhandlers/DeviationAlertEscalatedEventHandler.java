package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertEscalatedEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertEscalatedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes the escalation of an alert to the other bounded contexts.
 */
@Slf4j
@Service
public class DeviationAlertEscalatedEventHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DeviationAlertEscalatedEventHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(DeviationAlertEscalatedEvent.class)
    public void on(DeviationAlertEscalatedEvent event) {
        log.warn("Deviation alert escalated. alertId={}, parameterName={}, severity={}, deviations={}",
                event.alertId(), event.parameterName(), event.severity(), event.deviationCount());
        applicationEventPublisher.publishEvent(DeviationAlertEscalatedIntegrationEvent.from(event));
    }
}
