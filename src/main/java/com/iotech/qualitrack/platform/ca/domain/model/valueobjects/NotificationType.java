package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

/**
 * What a notification tells: a step of the life cycle of a deviation alert or a quality decision on a batch.
 */
public enum NotificationType {
    ALERT_OPENED,
    ALERT_ESCALATED,
    ALERT_ACKNOWLEDGED,
    ALERT_RESOLVED,
    BATCH_RELEASED,
    BATCH_REJECTED;

    public ComplianceEventSubject subject() {
        return switch (this) {
            case ALERT_OPENED, ALERT_ESCALATED, ALERT_ACKNOWLEDGED, ALERT_RESOLVED -> ComplianceEventSubject.ALERT;
            case BATCH_RELEASED, BATCH_REJECTED -> ComplianceEventSubject.BATCH;
        };
    }
}
