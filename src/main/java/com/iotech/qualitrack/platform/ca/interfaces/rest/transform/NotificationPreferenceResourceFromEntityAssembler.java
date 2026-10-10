package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.NotificationPreferenceResource;

/**
 * Assembler to convert NotificationPreference domain entities into REST resources.
 */
public class NotificationPreferenceResourceFromEntityAssembler {

    public static NotificationPreferenceResource toResourceFromEntity(NotificationPreference entity) {
        return new NotificationPreferenceResource(
                entity.getId(),
                entity.getUserId(),
                entity.getEmailEnabled(),
                entity.getInAppEnabled(),
                entity.getMinimumSeverity()
        );
    }
}