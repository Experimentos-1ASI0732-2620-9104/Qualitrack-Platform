package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.NotificationResource;

/**
 * Maps notifications to REST resources.
 */
public final class NotificationResourceFromEntityAssembler {

    private NotificationResourceFromEntityAssembler() {
    }

    public static NotificationResource toResourceFromEntity(Notification notification) {
        var content = notification.getContent();
        return new NotificationResource(notification.getId(), content.type().name(),
                content.severity() == null ? null : content.severity().name(), content.subject().name(),
                content.subjectId(), content.environmentName(), content.subjectName(), content.parameterName(),
                content.recordedValue(), content.unit(), content.actorName(), content.note(),
                notification.getOccurredAt().toString(),
                notification.getReadAt() == null ? null : notification.getReadAt().toString());
    }
}
