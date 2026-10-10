package com.iotech.qualitrack.platform.ca.domain.model.entities;

import com.iotech.qualitrack.platform.ca.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import lombok.Getter;

import java.util.Objects;

/**
 * The NotificationPreference domain entity.
 *
 * <p>Represents the notification delivery preferences configured by a user
 * for compliance and deviation alerts.</p>
 */
@Getter
public class NotificationPreference {

    /**
     * The unique internal numeric identifier for the preference record.
     */
    private Long id;

    /**
     * The numeric identifier of the user who owns these preferences.
     */
    private Long userId;

    /**
     * Indicates whether email notifications are enabled.
     */
    private Boolean emailEnabled;

    /**
     * Indicates whether in-app notifications are enabled.
     */
    private Boolean inAppEnabled;

    /**
     * The minimum severity required to notify the user.
     */
    private AlertSeverity minimumSeverity;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public NotificationPreference() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a NotificationPreference entity from persistence data.
     *
     * @param id The unique numeric ID.
     * @param userId The owner user ID.
     * @param emailEnabled Whether email notifications are enabled.
     * @param inAppEnabled Whether in-app notifications are enabled.
     * @param minimumSeverity The minimum severity threshold.
     */
    public NotificationPreference(
            Long id,
            Long userId,
            Boolean emailEnabled,
            Boolean inAppEnabled,
            AlertSeverity minimumSeverity
    ) {
        this.id = id;
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
        this.minimumSeverity = minimumSeverity;
    }

    /**
     * Constructs a new NotificationPreference with default delivery settings.
     *
     * @param userId The owner user ID. Cannot be null or less than 1.
     */
    public NotificationPreference(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId cannot be null or less than 1");
        }

        this.userId = userId;
        this.emailEnabled = true;
        this.inAppEnabled = true;
        this.minimumSeverity = AlertSeverity.WARNING;
    }

    /**
     * Applies updated notification preference values.
     *
     * @param command The command containing the updated preference data.
     */
    public void update(UpdateNotificationPreferenceCommand command) {
        Objects.requireNonNull(command, "Update notification preference command is required");

        this.emailEnabled = Objects.requireNonNull(command.emailEnabled(), "emailEnabled is required");
        this.inAppEnabled = Objects.requireNonNull(command.inAppEnabled(), "inAppEnabled is required");
        this.minimumSeverity = Objects.requireNonNull(command.minimumSeverity(), "minimumSeverity is required");
    }

    /**
     * Whether a notification reaches the bell of the user. The minimum severity applies to alerts; quality decisions
     * on batches always reach it.
     *
     * @param alertSeverity severity of the alert, or null for a notification that is not about an alert
     */
    public boolean wantsInApp(AlertSeverity alertSeverity) {
        return Boolean.TRUE.equals(inAppEnabled) && (alertSeverity == null || alertSeverity.isAtLeast(minimumSeverity));
    }

    /**
     * Whether the user receives the e-mail notice of an alert. Only critical alerts are e-mailed (US84).
     */
    public boolean wantsEmail(AlertSeverity alertSeverity) {
        return Boolean.TRUE.equals(emailEnabled) && alertSeverity == AlertSeverity.CRITICAL
                && alertSeverity.isAtLeast(minimumSeverity);
    }
}