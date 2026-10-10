package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.commands.GenerateBatchReportCommand;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.GenerateBatchReportResource;

/**
 * Assembler that transforms batch report REST resources into application commands.
 */
public final class GenerateBatchReportCommandFromResourceAssembler {

    private GenerateBatchReportCommandFromResourceAssembler() {
    }

    /**
     * Converts a batch report resource into a command.
     *
     * @param batchId the batch numeric identifier from the request path
     * @param resource the batch report request resource
     * @param requestedBy the authenticated user
     * @return the batch report generation command
     */
    public static GenerateBatchReportCommand toCommandFromResource(
            Long batchId,
            GenerateBatchReportResource resource,
            Long requestedBy
    ) {
        return new GenerateBatchReportCommand(
                batchId,
                resource.includeDeviations(),
                resource.format(),
                requestedBy
        );
    }
}