package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

/**
 * Maps notifications between the domain and the persistence entity.
 */
public final class NotificationPersistenceAssembler {

    private NotificationPersistenceAssembler() {
    }

    public static Notification toDomainFromPersistence(NotificationPersistenceEntity entity) {
        var content = new NotificationContent(entity.getType(), entity.getSeverity(), entity.getSubjectId(),
                entity.getEnvironmentName(), entity.getSubjectName(), entity.getParameterName(), entity.getRecordedValue(),
                entity.getUnit(), entity.getActorName(), entity.getNote());
        return new Notification(entity.getId(), entity.getRecipientUserId(), entity.getLaboratoryId(), content,
                entity.getOccurredAt(), entity.getReadAt());
    }

    public static NotificationPersistenceEntity toPersistenceFromDomain(Notification notification,
                                                                        NotificationPersistenceEntity entity) {
        var content = notification.getContent();
        entity.setId(notification.getId());
        entity.setRecipientUserId(notification.getRecipientUserId());
        entity.setLaboratoryId(notification.getLaboratoryId());
        entity.setType(content.type());
        entity.setSeverity(content.severity());
        entity.setSubjectId(content.subjectId());
        entity.setEnvironmentName(content.environmentName());
        entity.setSubjectName(content.subjectName());
        entity.setParameterName(content.parameterName());
        entity.setRecordedValue(content.recordedValue());
        entity.setUnit(content.unit());
        entity.setActorName(content.actorName());
        entity.setNote(content.note());
        entity.setOccurredAt(notification.getOccurredAt());
        entity.setReadAt(notification.getReadAt());
        return entity;
    }
}
