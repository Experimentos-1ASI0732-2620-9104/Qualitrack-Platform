package com.iotech.qualitrack.platform.ca.interfaces.events;

import com.iotech.qualitrack.platform.ca.domain.model.events.NotificationPreferenceUpdatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Integration event published by the Compliance and Alerts bounded context when notification preferences are updated.
 *
 * <p>This event supports audit and notification synchronization without exposing
 * CA internal domain events.</p>
 *
 * @param preferenceId the notification preference identifier
 * @param userId the owner user identifier
 * @param emailEnabled whether email notifications are enabled
 * @param inAppEnabled whether in-app notifications are enabled
 * @param minimumSeverity the minimum severity required for notifications
 */
public record NotificationPreferenceUpdatedIntegrationEvent(
        Long preferenceId,
        Long userId,
        Boolean emailEnabled,
        Boolean inAppEnabled,
        AlertSeverity minimumSeverity
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal notification preference updated domain event
     * @return the integration event
     */
    public static NotificationPreferenceUpdatedIntegrationEvent from(
            NotificationPreferenceUpdatedEvent event
    ) {
        return new NotificationPreferenceUpdatedIntegrationEvent(
                event.preferenceId(),
                event.userId(),
                event.emailEnabled(),
                event.inAppEnabled(),
                event.minimumSeverity()
        );
    }
}