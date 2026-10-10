package com.iotech.qualitrack.platform.tracking.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalDeviationDetectedEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalDeviationDetectedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Delivers detected deviations to Compliance &amp; Alerting.
 */
@Service
@Slf4j
public class EnvironmentalDeviationDetectedEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public EnvironmentalDeviationDetectedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(EnvironmentalDeviationDetectedEvent.class)
    public void on(EnvironmentalDeviationDetectedEvent event) {
        log.warn("Environmental deviation: device={}, metric={}, value={} {}, state={}, limit={}", event.deviceId(),
                event.metric(), event.value(), event.unit(), event.state(), event.thresholdValue());
        eventPublisher.publishEvent(EnvironmentalDeviationDetectedIntegrationEvent.from(event));
    }
}
