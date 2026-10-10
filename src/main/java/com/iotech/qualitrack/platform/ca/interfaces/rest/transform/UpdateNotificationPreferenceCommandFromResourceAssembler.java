package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.UpdateNotificationPreferenceResource;

/**
 * Assembler to convert UpdateNotificationPreferenceResource into UpdateNotificationPreferenceCommand.
 */
public class UpdateNotificationPreferenceCommandFromResourceAssembler {

    public static UpdateNotificationPreferenceCommand toCommandFromResource(
            Long userId,
            UpdateNotificationPreferenceResource resource
    ) {
        return new UpdateNotificationPreferenceCommand(
                userId,
                resource.emailEnabled(),
                resource.inAppEnabled(),
                resource.minimumSeverity()
        );
    }
}