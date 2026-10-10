package com.iotech.qualitrack.platform.ca.domain.model.commands;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

/**
 * Command to request the update of notification preferences for a user.
 *
 * @param userId The numeric identifier of the user. Cannot be null or less than 1.
 * @param emailEnabled Indicates whether email notifications are enabled.
 * @param inAppEnabled Indicates whether in-app notifications are enabled.
 * @param minimumSeverity The minimum alert severity required to notify the user. Cannot be null.
 */
public record UpdateNotificationPreferenceCommand(
        Long userId,
        Boolean emailEnabled,
        Boolean inAppEnabled,
        AlertSeverity minimumSeverity
) {
    /**
     * Compact constructor for UpdateNotificationPreferenceCommand.
     * Enforces Fail-Fast validation.
     */
    public UpdateNotificationPreferenceCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId cannot be null or less than 1");
        }
        if (emailEnabled == null) {
            throw new IllegalArgumentException("emailEnabled cannot be null");
        }
        if (inAppEnabled == null) {
            throw new IllegalArgumentException("inAppEnabled cannot be null");
        }
        if (minimumSeverity == null) {
            throw new IllegalArgumentException("minimumSeverity cannot be null");
        }
    }
}