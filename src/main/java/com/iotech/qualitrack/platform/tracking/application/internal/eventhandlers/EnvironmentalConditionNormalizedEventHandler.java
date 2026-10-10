package com.iotech.qualitrack.platform.tracking.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalConditionNormalizedEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalConditionNormalizedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Delivers the return to normal of a metric to Compliance &amp; Alerting.
 */
@Service
@Slf4j
public class EnvironmentalConditionNormalizedEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public EnvironmentalConditionNormalizedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(EnvironmentalConditionNormalizedEvent.class)
    public void on(EnvironmentalConditionNormalizedEvent event) {
        log.info("Environmental condition back to normal: device={}, metric={}, value={} {}", event.deviceId(),
                event.metric(), event.value(), event.unit());
        eventPublisher.publishEvent(EnvironmentalConditionNormalizedIntegrationEvent.from(event));
    }
}
