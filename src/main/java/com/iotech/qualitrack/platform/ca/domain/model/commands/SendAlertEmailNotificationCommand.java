package com.iotech.qualitrack.platform.ca.domain.model.commands;

/**
 * Command to e-mail the people of the laboratory who enabled e-mail notices about an open critical alert (US84, TS78).
 *
 * @param alertId the alert
 * @param requestedBy the person who asked for it, who is not e-mailed; null when the system sends it on its own
 */
public record SendAlertEmailNotificationCommand(Long alertId, Long requestedBy) {
    public SendAlertEmailNotificationCommand {
        if (alertId == null || alertId <= 0) throw new IllegalArgumentException("alertId cannot be null or less than 1");
    }
}
