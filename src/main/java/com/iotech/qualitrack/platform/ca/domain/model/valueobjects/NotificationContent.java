package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

/**
 * What happened, with the values the message shows. The text is composed by each client in the language of the
 * person; unused values are null.
 *
 * @param type what happened
 * @param severity severity of the alert, or null for a batch
 * @param subjectId the alert or the batch
 * @param environmentName environment of the alert
 * @param subjectName device of the alert or number of the batch
 * @param parameterName variable of the alert, for example TEMPERATURE
 * @param recordedValue value that deviated
 * @param unit unit of the value
 * @param actorName person who acknowledged, resolved, released or rejected
 * @param note rejection reason or resolution notes
 */
public record NotificationContent(NotificationType type, AlertSeverity severity, Long subjectId, String environmentName,
                                  String subjectName, String parameterName, Double recordedValue, String unit,
                                  String actorName, String note) {
    public NotificationContent {
        if (type == null) throw new IllegalArgumentException("The notification type is required");
        if (subjectId == null || subjectId <= 0) throw new IllegalArgumentException("The notification subject is required");
        if (type.subject() == ComplianceEventSubject.ALERT && severity == null) {
            throw new IllegalArgumentException("An alert notification needs the severity of the alert");
        }
        if (note != null && note.length() > 500) note = note.substring(0, 500);
    }

    public ComplianceEventSubject subject() {
        return type.subject();
    }
}
