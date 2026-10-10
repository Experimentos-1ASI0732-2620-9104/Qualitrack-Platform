package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.ResolveAlertResource;

/**
 * Assembler to convert ResolveAlertResource into ResolveAlertCommand.
 */
public class ResolveAlertCommandFromResourceAssembler {

    /**
     * @param alertId    alert being resolved
     * @param resolvedBy authenticated user resolving it
     * @param resource   resolution notes
     * @throws IllegalArgumentException when the notes are missing (400)
     */
    public static ResolveAlertCommand toCommandFromResource(Long alertId, Long resolvedBy, ResolveAlertResource resource) {
        return new ResolveAlertCommand(
                alertId,
                resolvedBy,
                resource == null ? null : resource.resolutionNotes()
        );
    }
}
