package com.iotech.qualitrack.platform.ca.domain.model.events;

import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Domain event published when a user's notification preferences are updated.
 *
 * <p>Can be used by audit or notification workflows to record preference changes
 * and keep delivery behavior traceable.</p>
 *
 * @param preferenceId The numeric identity of the notification preference record.
 * @param userId The numeric identity of the user who owns the preferences.
 * @param emailEnabled Whether email notifications are enabled.
 * @param inAppEnabled Whether in-app notifications are enabled.
 * @param minimumSeverity The minimum severity required to notify the user.
 */
public record NotificationPreferenceUpdatedEvent(
        Long preferenceId,
        Long userId,
        Boolean emailEnabled,
        Boolean inAppEnabled,
        AlertSeverity minimumSeverity
) {
    /**
     * Convenience factory that extracts all needed fields from an updated {@link NotificationPreference}.
     *
     * @param preference the updated notification preference
     * @return a fully populated {@link NotificationPreferenceUpdatedEvent}
     */
    public static NotificationPreferenceUpdatedEvent from(NotificationPreference preference) {
        return new NotificationPreferenceUpdatedEvent(
                preference.getId(),
                preference.getUserId(),
                preference.getEmailEnabled(),
                preference.getInAppEnabled(),
                preference.getMinimumSeverity()
        );
    }
}