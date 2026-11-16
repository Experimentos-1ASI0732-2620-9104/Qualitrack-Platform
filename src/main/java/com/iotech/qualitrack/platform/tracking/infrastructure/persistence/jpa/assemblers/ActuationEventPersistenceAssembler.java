package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.ActuationEventPersistenceEntity;

/**
 * Static assembler between actuation event domain and persistence representations.
 */
public final class ActuationEventPersistenceAssembler {

    private ActuationEventPersistenceAssembler() {
    }

    /**
     * @return the domain action, with the creation time of the row as reception time; null for a null entity
     */
    public static ActuationEvent toDomainFromPersistence(ActuationEventPersistenceEntity entity) {
        if (entity == null) return null;
        return new ActuationEvent(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(),
                entity.getDeviceId(), entity.getAction(), entity.getTriggerMetric(), entity.getTriggerState(),
                entity.getResult(), entity.getOccurredAt(), entity.getProfileVersion(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toInstant());
    }

    /**
     * @return a new entity; the audit columns are filled by JPA when it is saved; null for a null event
     */
    public static ActuationEventPersistenceEntity toPersistenceFromDomain(ActuationEvent event) {
        if (event == null) return null;
        var entity = new ActuationEventPersistenceEntity();
        if (event.getId() != null) entity.setId(event.getId());
        entity.setLaboratoryId(event.getLaboratoryId());
        entity.setEnvironmentId(event.getEnvironmentId());
        entity.setDeviceId(event.getDeviceId());
        entity.setAction(event.getAction());
        entity.setTriggerMetric(event.getTriggerMetric());
        entity.setTriggerState(event.getTriggerState());
        entity.setResult(event.getResult());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setProfileVersion(event.getProfileVersion());
        return entity;
    }
}
