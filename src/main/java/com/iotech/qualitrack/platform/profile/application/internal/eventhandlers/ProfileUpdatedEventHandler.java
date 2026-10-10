package com.iotech.qualitrack.platform.profile.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.profile.domain.model.events.ProfileUpdatedEvent;
import com.iotech.qualitrack.platform.profile.interfaces.events.ProfileUpdatedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes the integration event of a profile whose full name changed.
 */
@Service
public class ProfileUpdatedEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    public ProfileUpdatedEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ProfileUpdatedEvent.class)
    public void on(ProfileUpdatedEvent event) {
        eventPublisher.publishEvent(ProfileUpdatedIntegrationEvent.from(event));
    }
}
