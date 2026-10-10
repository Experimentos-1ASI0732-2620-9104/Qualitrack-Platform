package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.commands.GenerateInventoryReportCommand;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.GenerateInventoryReportResource;

/**
 * Assembler that transforms inventory report requests into application commands.
 */
public final class GenerateInventoryReportCommandFromResourceAssembler {
    private GenerateInventoryReportCommandFromResourceAssembler() {
    }

    /**
     * @throws IllegalArgumentException when the format is missing (400)
     */
    public static GenerateInventoryReportCommand toCommandFromResource(Long laboratoryId,
            GenerateInventoryReportResource resource, Long requestedBy) {
        if (resource == null) throw new IllegalArgumentException("The report request is required");
        return new GenerateInventoryReportCommand(laboratoryId, resource.environmentId(), resource.format(), requestedBy);
    }
}
