package com.iotech.qualitrack.platform.tracking.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.tracking.domain.model.events.MeasurementRecordedEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.events.MeasurementRecordedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes recorded readings to the other bounded contexts.
 */
@Service
@Slf4j
public class MeasurementRecordedEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public MeasurementRecordedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(MeasurementRecordedEvent.class)
    public void on(MeasurementRecordedEvent event) {
        log.debug("Measurement recorded: id={}, device={}, metric={}, state={}", event.measurementId(),
                event.deviceId(), event.metric(), event.state());
        eventPublisher.publishEvent(MeasurementRecordedIntegrationEvent.from(event));
    }
}
