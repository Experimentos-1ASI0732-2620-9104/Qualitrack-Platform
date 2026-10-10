package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.NotificationPreferencePersistenceEntity;

/**
 * Static assembler between notification preference domain and persistence representations.
 */
public final class NotificationPreferencePersistenceAssembler {

    private NotificationPreferencePersistenceAssembler() {
    }

    public static NotificationPreference toDomainFromPersistence(NotificationPreferencePersistenceEntity entity) {
        if (entity == null) return null;

        return new NotificationPreference(
                entity.getId(),
                entity.getUserId(),
                entity.getEmailEnabled(),
                entity.getInAppEnabled(),
                entity.getMinimumSeverity()
        );
    }

    public static NotificationPreferencePersistenceEntity toPersistenceFromDomain(NotificationPreference preference) {
        if (preference == null) return null;

        var entity = new NotificationPreferencePersistenceEntity();

        if (preference.getId() != null) {
            entity.setId(preference.getId());
        }

        entity.setUserId(preference.getUserId());
        entity.setEmailEnabled(preference.getEmailEnabled());
        entity.setInAppEnabled(preference.getInAppEnabled());
        entity.setMinimumSeverity(preference.getMinimumSeverity());

        return entity;
    }
}