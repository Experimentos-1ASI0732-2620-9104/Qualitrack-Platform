package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.AssignEnvironmentUsageCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.AssignEnvironmentUsageResource;

import java.util.Arrays;
import java.util.Locale;

/**
 * Assembler to convert an AssignEnvironmentUsageResource to an AssignEnvironmentUsageCommand.
 */
public final class AssignEnvironmentUsageCommandFromResourceAssembler {

    private AssignEnvironmentUsageCommandFromResourceAssembler() {
    }

    /**
     * Converts the request into a command.
     *
     * @throws IllegalArgumentException when the usage is not one of the allowed values
     */
    public static AssignEnvironmentUsageCommand toCommandFromResource(Long laboratoryId, Long environmentId,
                                                                      AssignEnvironmentUsageResource resource) {
        return new AssignEnvironmentUsageCommand(laboratoryId, environmentId, toUsage(resource.usage()));
    }

    private static EnvironmentUsage toUsage(String value) {
        try {
            return EnvironmentUsage.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unsupported environment usage '%s'. Allowed values: %s"
                    .formatted(value, Arrays.toString(EnvironmentUsage.values())));
        }
    }
}
