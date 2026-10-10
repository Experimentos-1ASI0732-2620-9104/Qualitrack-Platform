package com.iotech.qualitrack.platform.profile.interfaces.events;

import com.iotech.qualitrack.platform.profile.domain.model.events.ProfileUpdatedEvent;

/**
 * Integration event published when a person changes the full name of the profile, so that the staff list of the
 * laboratory shows it.
 */
public record ProfileUpdatedIntegrationEvent(Long userId, String fullName) {
    public static ProfileUpdatedIntegrationEvent from(ProfileUpdatedEvent event) {
        return new ProfileUpdatedIntegrationEvent(event.userId(), event.fullName());
    }
}
