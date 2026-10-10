package com.iotech.qualitrack.platform.iam.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.iam.domain.model.events.UserAccountUpdatedEvent;
import com.iotech.qualitrack.platform.iam.interfaces.events.UserAccountUpdatedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes the integration event of an account whose username or e-mail changed.
 */
@Service
public class UserAccountUpdatedEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    public UserAccountUpdatedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(UserAccountUpdatedEvent.class)
    public void on(UserAccountUpdatedEvent event) {
        eventPublisher.publishEvent(UserAccountUpdatedIntegrationEvent.from(event));
    }
}
