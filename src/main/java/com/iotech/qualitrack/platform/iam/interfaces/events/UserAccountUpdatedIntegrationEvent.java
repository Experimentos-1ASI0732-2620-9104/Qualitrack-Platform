package com.iotech.qualitrack.platform.iam.interfaces.events;

import com.iotech.qualitrack.platform.iam.domain.model.events.UserAccountUpdatedEvent;

/**
 * Integration event published when a user changes the username or the e-mail of the account, so that the contexts
 * keeping a copy of them (for example the staff of the laboratory) stay up to date.
 */
public record UserAccountUpdatedIntegrationEvent(Long userId, Long laboratoryId, String username, String email) {
    public static UserAccountUpdatedIntegrationEvent from(UserAccountUpdatedEvent event) {
        return new UserAccountUpdatedIntegrationEvent(event.userId(), event.laboratoryId(), event.username(), event.email());
    }
}
