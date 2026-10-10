package com.iotech.qualitrack.platform.ca.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.CreateDeviationAlertResource;

/**
 * Assembler to convert CreateDeviationAlertResource into CreateDeviationAlertCommand.
 */
public final class CreateDeviationAlertCommandFromResourceAssembler {
    private CreateDeviationAlertCommandFromResourceAssembler() {
    }

    /**
     * @throws IllegalArgumentException when a required value is missing or the severity is unknown (400)
     */
    public static CreateDeviationAlertCommand toCommandFromResource(Long laboratoryId, Long environmentId,
                                                                    CreateDeviationAlertResource resource) {
        if (resource == null) throw new IllegalArgumentException("The deviation is required");
        AlertSeverity severity;
        try {
            severity = resource.severity() == null ? null : AlertSeverity.valueOf(resource.severity().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown severity: " + resource.severity());
        }
        return new CreateDeviationAlertCommand(laboratoryId, environmentId, resource.deviceId(), null,
                resource.parameterName(), resource.recordedValue(), resource.thresholdValue(), resource.unit(), severity,
                resource.detectedAt());
    }
}
