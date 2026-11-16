package com.iotech.qualitrack.platform.tracking.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalConditionNormalizedEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalConditionNormalizedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Delivers the return to normal of a metric to Compliance &amp; Alerting, which notes it in the open alert.
 *
 * <p>The domain event is translated into an integration event so the other contexts do not depend on the Tracking
 * domain model.</p>
 */
@Service
@Slf4j
public class EnvironmentalConditionNormalizedEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public EnvironmentalConditionNormalizedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Translates the domain event into {@link EnvironmentalConditionNormalizedIntegrationEvent} and publishes it.
     */
    @EventListener(EnvironmentalConditionNormalizedEvent.class)
    public void on(EnvironmentalConditionNormalizedEvent event) {
        log.info("Environmental condition back to normal: device={}, metric={}, value={} {}", event.deviceId(),
                event.metric(), event.value(), event.unit());
        eventPublisher.publishEvent(EnvironmentalConditionNormalizedIntegrationEvent.from(event));
    }
}
