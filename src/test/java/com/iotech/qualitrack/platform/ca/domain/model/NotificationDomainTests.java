package com.iotech.qualitrack.platform.ca.domain.model;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Who receives a notification and how it is read (US83, US84).
 */
class NotificationDomainTests {

    @Test
    void preferencesDecideWhichNoticesReachTheBellAndTheEmail() {
        var defaults = new NotificationPreference(7L);
        assertThat(defaults.wantsInApp(AlertSeverity.WARNING)).isTrue();
        assertThat(defaults.wantsInApp(AlertSeverity.LOW)).isFalse();
        assertThat(defaults.wantsInApp(null)).isTrue();
        assertThat(defaults.wantsEmail(AlertSeverity.CRITICAL)).isTrue();
        assertThat(defaults.wantsEmail(AlertSeverity.WARNING)).isFalse();

        var criticalOnly = new NotificationPreference(7L);
        criticalOnly.update(new UpdateNotificationPreferenceCommand(7L, false, true, AlertSeverity.CRITICAL));
        assertThat(criticalOnly.wantsInApp(AlertSeverity.WARNING)).isFalse();
        assertThat(criticalOnly.wantsInApp(AlertSeverity.CRITICAL)).isTrue();
        assertThat(criticalOnly.wantsInApp(null)).isTrue();
        assertThat(criticalOnly.wantsEmail(AlertSeverity.CRITICAL)).isFalse();

        var silent = new NotificationPreference(7L);
        silent.update(new UpdateNotificationPreferenceCommand(7L, true, false, AlertSeverity.LOW));
        assertThat(silent.wantsInApp(AlertSeverity.CRITICAL)).isFalse();
        assertThat(silent.wantsInApp(null)).isFalse();
    }

    @Test
    void aNotificationIsReadOnceAndAlertNoticesCarryTheirSeverity() {
        var content = new NotificationContent(NotificationType.BATCH_RELEASED, null, 3L, null, "PB-1", null, null, null,
                "Ana Torres", null);
        var notification = new Notification(7L, 1L, content, Instant.parse("2026-10-04T15:00:00Z"));
        var firstRead = Instant.parse("2026-10-04T15:05:00Z");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.markAsRead(firstRead)).isTrue();
        assertThat(notification.markAsRead(firstRead.plusSeconds(60))).isFalse();
        assertThat(notification.getReadAt()).isEqualTo(firstRead);
        assertThat(notification.belongsTo(7L)).isTrue();

        assertThatThrownBy(() -> new NotificationContent(NotificationType.ALERT_OPENED, null, 3L, null, null, null,
                null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new NotificationContent(NotificationType.ALERT_RESOLVED, AlertSeverity.WARNING, 3L, null, null, null,
                null, null, null, "x".repeat(800)).note()).hasSize(500);
    }
}
