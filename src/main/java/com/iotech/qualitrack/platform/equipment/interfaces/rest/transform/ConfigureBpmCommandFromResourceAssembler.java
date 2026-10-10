package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.ConfigureBpmParametersCommand;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.ConfigureBpmResource;

/**
 * Assembler to convert a ConfigureBpmResource to a ConfigureBpmParametersCommand.
 */
public class ConfigureBpmCommandFromResourceAssembler {

    /**
     * @throws IllegalArgumentException when a value is missing or the range is not valid (400)
     */
    public static ConfigureBpmParametersCommand toCommandFromResource(Long equipmentId, String parameterName,
                                                                      ConfigureBpmResource resource) {
        if (resource == null) throw new IllegalArgumentException("The range of the parameter is required");
        return new ConfigureBpmParametersCommand(
                equipmentId,
                parameterName,
                resource.minValue(),
                resource.maxValue(),
                resource.unit()
        );
    }
}
